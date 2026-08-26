$ErrorActionPreference = 'Stop'
$baseUrl = 'http://localhost:8080'
$script:results = [System.Collections.Generic.List[object]]::new()

function Invoke-Api {
    param([string]$Method, [string]$Path, $Body = $null, [string]$Token = '')
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $params = @{ Method = $Method; Uri = "$baseUrl$Path"; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 8 -Compress }
    }
    try {
        $response = Invoke-WebRequest @params
        $json = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
        return [pscustomobject]@{ Status = [int]$response.StatusCode; Json = $json; Raw = $response.Content }
    } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        $raw = $_.ErrorDetails.Message
        if (-not $raw -and $_.Exception.Response) {
            try {
                $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
                $raw = $reader.ReadToEnd()
                $reader.Dispose()
            } catch {}
        }
        $json = $null
        if ($raw) { try { $json = $raw | ConvertFrom-Json } catch {} }
        return [pscustomobject]@{ Status = $status; Json = $json; Raw = $raw }
    }
}

function Check {
    param([string]$Name, [bool]$Condition, [string]$Evidence)
    $script:results.Add([pscustomobject]@{ Name = $Name; Passed = $Condition; Evidence = $Evidence })
    $mark = if ($Condition) { 'PASS' } else { 'FAIL' }
    Write-Host "[$mark] $Name - $Evidence"
}

function Register([string]$Email, [string]$Role) {
    $r = Invoke-Api POST '/api/auth/register' @{ email=$Email; password='Reviewer123!'; name=$Email.Split('@')[0]; role=$Role }
    Check "Register $Role $Email" ($r.Status -eq 201 -and $r.Json.email -eq $Email) "HTTP $($r.Status)"
    return $r.Json
}

function Login([string]$Email) {
    $r = Invoke-Api POST '/api/auth/login' @{ email=$Email; password='Reviewer123!' }
    Check "Login $Email" ($r.Status -eq 200 -and $r.Json.token -and $r.Json.expiresAt) "HTTP $($r.Status), token and expiry returned"
    return $r.Json.token
}

$r = Invoke-Api GET '/api/tickets'
Check 'Unauthenticated request is rejected' ($r.Status -eq 401 -and $r.Json.status -eq 401) "HTTP $($r.Status), shared error status=$($r.Json.status)"

$r = Invoke-Api POST '/api/auth/register' @{ email='bad'; password='short'; name=''; role=$null }
Check 'Registration reports all invalid fields' ($r.Status -eq 400 -and $r.Json.fieldErrors.Count -ge 4) "HTTP $($r.Status), fieldErrors=$($r.Json.fieldErrors.Count)"

$customer1 = Register 'review.customer1@example.com' 'CUSTOMER'
$customer2 = Register 'review.customer2@example.com' 'CUSTOMER'
$agent1 = Register 'review.agent1@example.com' 'AGENT'
$agent2 = Register 'review.agent2@example.com' 'AGENT'

$r = Invoke-Api POST '/api/auth/register' @{ email='review.customer1@example.com'; password='Reviewer123!'; name='Duplicate'; role='CUSTOMER' }
Check 'Duplicate email conflicts' ($r.Status -eq 409) "HTTP $($r.Status)"
$r = Invoke-Api POST '/api/auth/login' @{ email='review.customer1@example.com'; password='wrong-password' }
Check 'Wrong password is rejected cleanly' ($r.Status -eq 401 -and $r.Json.message -eq 'Invalid email or password') "HTTP $($r.Status), message=$($r.Json.message)"

$customer1Token = Login 'review.customer1@example.com'
$customer2Token = Login 'review.customer2@example.com'
$agent1Token = Login 'review.agent1@example.com'
$agent2Token = Login 'review.agent2@example.com'

$r = Invoke-Api GET '/api/tickets' $null ($customer1Token + 'tampered')
Check 'Tampered JWT is rejected' ($r.Status -eq 401) "HTTP $($r.Status)"
$r = Invoke-Api GET '/api/does-not-exist' $null $customer1Token
Check 'Unknown endpoint returns not found' ($r.Status -eq 404 -and $r.Json.status -eq 404) "HTTP $($r.Status)"
$r = Invoke-Api PUT '/api/tickets' @{} $customer1Token
Check 'Unsupported HTTP method is explicit' ($r.Status -eq 405 -and $r.Json.status -eq 405) "HTTP $($r.Status)"
$r = Invoke-Api POST '/api/tickets' @{title='Agent ticket';description='Not allowed';category='GENERAL'} $agent1Token
Check 'Agent cannot create a customer ticket' ($r.Status -eq 403) "HTTP $($r.Status)"
$r = Invoke-Api POST '/api/tickets' @{title=' ';description=' ';priority='LOW'} $customer1Token
Check 'Ticket validation returns every field error' ($r.Status -eq 400 -and $r.Json.fieldErrors.Count -ge 3) "HTTP $($r.Status), fieldErrors=$($r.Json.fieldErrors.Count)"
$r = Invoke-Api POST '/api/tickets' '{"title":"Bad enum","description":"Details","category":"NOPE"}' $customer1Token
Check 'Invalid category enum has a field error' ($r.Status -eq 400 -and $r.Json.fieldErrors[0].field -eq 'category') "HTTP $($r.Status), field=$($r.Json.fieldErrors[0].field)"

$r = Invoke-Api POST '/api/tickets' @{title='Laptop will not boot';description='Black screen after update';category='TECHNICAL';customerId=$customer2.id} $customer1Token
$ticket1 = $r.Json
Check 'Create ticket uses server identity and defaults' ($r.Status -eq 201 -and $ticket1.customerId -eq $customer1.id -and $ticket1.status -eq 'OPEN' -and $ticket1.priority -eq 'MEDIUM' -and $null -eq $ticket1.assignedAgentId) "HTTP $($r.Status), owner=$($ticket1.customerId), status=$($ticket1.status), priority=$($ticket1.priority)"
$r = Invoke-Api POST '/api/tickets' @{title='Invoice mismatch';description='Unexpected amount';category='BILLING';priority='URGENT'} $customer1Token
$ticket2 = $r.Json
Check 'Explicit category and priority persist' ($r.Status -eq 201 -and $ticket2.category -eq 'BILLING' -and $ticket2.priority -eq 'URGENT') "HTTP $($r.Status)"
$r = Invoke-Api POST '/api/tickets' @{title='Second customer';description='Private account issue';category='ACCOUNT'} $customer2Token
$otherTicket = $r.Json
Check 'Second customer creates isolated ticket' ($r.Status -eq 201 -and $otherTicket.customerId -eq $customer2.id) "HTTP $($r.Status)"

$r = Invoke-Api GET '/api/tickets?size=1&page=0&sort=createdAt,asc' $null $customer1Token
$page0Id = $r.Json.content[0].id
Check 'Customer list is paginated' ($r.Status -eq 200 -and $r.Json.page.size -eq 1 -and $r.Json.page.totalElements -eq 2) "HTTP $($r.Status), size=$($r.Json.page.size), total=$($r.Json.page.totalElements)"
$r = Invoke-Api GET '/api/tickets?size=1&page=1&sort=createdAt,asc' $null $customer1Token
Check 'Second page has no duplicate' ($r.Status -eq 200 -and $r.Json.content.Count -eq 1 -and $r.Json.content[0].id -ne $page0Id) "HTTP $($r.Status)"
$r = Invoke-Api GET '/api/tickets?status=OPEN&priority=URGENT&category=BILLING' $null $customer1Token
Check 'Combined list filters work in caller scope' ($r.Status -eq 200 -and $r.Json.page.totalElements -eq 1 -and $r.Json.content[0].id -eq $ticket2.id) "HTTP $($r.Status), matches=$($r.Json.page.totalElements)"
$r = Invoke-Api GET "/api/tickets/$($otherTicket.id)" $null $customer1Token
Check 'Customer cannot view another customer ticket' ($r.Status -eq 403) "HTTP $($r.Status)"
$missingTicket = [guid]::NewGuid().ToString()
$r = Invoke-Api GET "/api/tickets/$missingTicket" $null $customer1Token
Check 'Missing ticket returns not found' ($r.Status -eq 404) "HTTP $($r.Status)"
$r = Invoke-Api GET '/api/tickets/not-a-uuid' $null $customer1Token
Check 'Malformed ticket id is a client error' ($r.Status -eq 400) "HTTP $($r.Status)"

$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/assign" @{} $customer1Token
Check 'Customer cannot assign tickets' ($r.Status -eq 403) "HTTP $($r.Status)"
$r = Invoke-Api GET '/api/tickets' $null $agent1Token
Check 'Agent list initially contains only assigned tickets' ($r.Status -eq 200 -and $r.Json.page.totalElements -eq 0) "HTTP $($r.Status), total=$($r.Json.page.totalElements)"
$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/assign" @{} $agent1Token
$ticket1 = $r.Json
Check 'Agent can self-assign without status change' ($r.Status -eq 200 -and $ticket1.assignedAgentId -eq $agent1.id -and $ticket1.status -eq 'OPEN') "HTTP $($r.Status), agent=$($ticket1.assignedAgentId), status=$($ticket1.status)"
$r = Invoke-Api GET "/api/tickets/$($ticket1.id)/history" $null $agent1Token
Check 'Creation and assignment history are recorded' ($r.Status -eq 200 -and $r.Json.Count -eq 2 -and $r.Json[0].eventType -eq 'STATUS_CHANGE' -and $r.Json[1].eventType -eq 'ASSIGNMENT') "HTTP $($r.Status), events=$($r.Json.Count)"
$r = Invoke-Api GET "/api/tickets/$($ticket1.id)" $null $agent2Token
Check 'Unassigned agent cannot view ticket' ($r.Status -eq 403) "HTTP $($r.Status)"

$r = Invoke-Api POST "/api/tickets/$($ticket1.id)/comments" @{body='Agent diagnostic note'} $agent1Token
$comment1 = $r.Json
Check 'Assigned agent can comment' ($r.Status -eq 201 -and $comment1.authorRole -eq 'AGENT') "HTTP $($r.Status)"
$r = Invoke-Api POST "/api/tickets/$($ticket1.id)/comments" @{body='Customer follow-up'} $customer1Token
Check 'Ticket customer can comment' ($r.Status -eq 201 -and $r.Json.authorRole -eq 'CUSTOMER') "HTTP $($r.Status)"
$r = Invoke-Api POST "/api/tickets/$($ticket1.id)/comments" @{body='Intrusion'} $customer2Token
Check 'Other customer cannot comment' ($r.Status -eq 403) "HTTP $($r.Status)"
$r = Invoke-Api GET "/api/tickets/$($ticket1.id)/comments" $null $agent2Token
Check 'Other agent cannot list comments' ($r.Status -eq 403) "HTTP $($r.Status)"
$r = Invoke-Api GET "/api/tickets/$($ticket1.id)/comments" $null $customer1Token
Check 'Comments are chronological with author metadata' ($r.Status -eq 200 -and $r.Json.Count -eq 2 -and $r.Json[0].body -eq 'Agent diagnostic note' -and $r.Json[1].body -eq 'Customer follow-up' -and $r.Json[0].authorName) "HTTP $($r.Status), comments=$($r.Json.Count)"
$longBody = 'x' * 5001
$r = Invoke-Api POST "/api/tickets/$($ticket1.id)/comments" @{body=$longBody} $customer1Token
Check 'Oversized comment is rejected' ($r.Status -eq 400) "HTTP $($r.Status)"

$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/priority" @{priority='HIGH'} $customer1Token
Check 'Customer cannot update priority' ($r.Status -eq 403) "HTTP $($r.Status)"
$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/priority" @{priority='HIGH'} $agent2Token
Check 'Unassigned agent cannot update priority' ($r.Status -eq 403) "HTTP $($r.Status)"
$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/priority" @{priority='HIGH'} $agent1Token
Check 'Assigned agent updates priority' ($r.Status -eq 200 -and $r.Json.priority -eq 'HIGH') "HTTP $($r.Status), priority=$($r.Json.priority)"
$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/priority" '{"priority":"IMPOSSIBLE"}' $agent1Token
Check 'Invalid priority enum has a field error' ($r.Status -eq 400 -and $r.Json.fieldErrors[0].field -eq 'priority') "HTTP $($r.Status), field=$($r.Json.fieldErrors[0].field)"

$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/status" @{status='IN_PROGRESS'} $customer1Token
Check 'Customer cannot start work' ($r.Status -eq 403) "HTTP $($r.Status)"
$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/status" @{status='IN_PROGRESS'} $agent2Token
Check 'Unassigned agent cannot change status' ($r.Status -eq 403) "HTTP $($r.Status)"
$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/status" @{status='RESOLVED'} $agent1Token
Check 'Invalid OPEN to RESOLVED transition conflicts' ($r.Status -eq 409 -and $r.Json.message -match 'OPEN.*RESOLVED') "HTTP $($r.Status), message=$($r.Json.message)"
$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/status" @{status='IN_PROGRESS';note=('n' * 1001)} $agent1Token
Check 'Oversized status note is rejected' ($r.Status -eq 400 -and $r.Json.fieldErrors[0].field -eq 'note') "HTTP $($r.Status), field=$($r.Json.fieldErrors[0].field)"
$r = Invoke-Api GET "/api/tickets/$($ticket1.id)/history" $null $agent1Token
Check 'Rejected transition writes no history' ($r.Status -eq 200 -and $r.Json.Count -eq 2) "events=$($r.Json.Count)"

$transitions = @(
    @('IN_PROGRESS',$agent1Token,'Agent OPEN to IN_PROGRESS'),
    @('OPEN',$agent1Token,'Agent IN_PROGRESS to OPEN'),
    @('IN_PROGRESS',$agent1Token,'Agent reopened work from OPEN'),
    @('RESOLVED',$agent1Token,'Agent resolves ticket'),
    @('REOPENED',$customer1Token,'Customer reopens resolved ticket'),
    @('IN_PROGRESS',$agent1Token,'Agent resumes reopened ticket'),
    @('RESOLVED',$agent1Token,'Agent resolves again'),
    @('CLOSED',$customer1Token,'Customer closes resolved ticket')
)
foreach ($step in $transitions) {
    $r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/status" @{status=$step[0];note=$step[2]} $step[1]
    Check $step[2] ($r.Status -eq 200 -and $r.Json.status -eq $step[0]) "HTTP $($r.Status), status=$($r.Json.status)"
}
$r = Invoke-Api PATCH "/api/tickets/$($ticket1.id)/status" @{status='REOPENED'} $agent1Token
Check 'CLOSED is terminal' ($r.Status -eq 409) "HTTP $($r.Status)"
$r = Invoke-Api GET "/api/tickets/$($ticket1.id)/history" $null $customer1Token
$ordered = $true
for ($i=1; $i -lt $r.Json.Count; $i++) { if ([datetime]$r.Json[$i].changedAt -lt [datetime]$r.Json[$i-1].changedAt) { $ordered = $false } }
Check 'Full history is complete and chronological' ($r.Status -eq 200 -and $r.Json.Count -eq 10 -and $ordered -and $r.Json[-1].toStatus -eq 'CLOSED') "HTTP $($r.Status), events=$($r.Json.Count), ordered=$ordered"

$r = Invoke-Api PATCH "/api/tickets/$($ticket2.id)/assign" @{agentId=$agent2.id} $agent1Token
Check 'Agent can assign a named agent' ($r.Status -eq 200 -and $r.Json.assignedAgentId -eq $agent2.id) "HTTP $($r.Status), agent=$($r.Json.assignedAgentId)"
$r = Invoke-Api GET "/api/tickets/$($ticket2.id)" $null $agent1Token
Check 'Previous agent loses access after reassignment' ($r.Status -eq 403) "HTTP $($r.Status)"
$r = Invoke-Api PATCH "/api/tickets/$($ticket2.id)/priority" @{priority='LOW'} $agent2Token
Check 'New assigned agent gains management access' ($r.Status -eq 200 -and $r.Json.priority -eq 'LOW') "HTTP $($r.Status)"
$r = Invoke-Api PATCH "/api/tickets/$($ticket2.id)/assign" @{agentId=$customer1.id} $agent2Token
Check 'Ticket cannot be assigned to customer' ($r.Status -eq 403) "HTTP $($r.Status)"
$r = Invoke-Api PATCH "/api/tickets/$($ticket2.id)/assign" @{agentId=[guid]::NewGuid().ToString()} $agent2Token
Check 'Unknown target agent returns not found' ($r.Status -eq 404) "HTTP $($r.Status)"
$r = Invoke-Api GET "/api/tickets/$($ticket2.id)/history" $null $customer2Token
Check 'Other customer cannot view history' ($r.Status -eq 403) "HTTP $($r.Status)"

$passed = @($results | Where-Object Passed).Count
$failed = @($results | Where-Object { -not $_.Passed }).Count
Write-Host "TOTAL=$($results.Count) PASSED=$passed FAILED=$failed"
if ($failed -gt 0) { exit 1 }
