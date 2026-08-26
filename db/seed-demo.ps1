$ErrorActionPreference = 'Stop'
$baseUrl = 'http://localhost:8080'

function Call-Api {
    param([string]$Method, [string]$Path, $Body, [string]$Token = '')
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $parameters = @{
        Method = $Method
        Uri = "$baseUrl$Path"
        ContentType = 'application/json'
        Headers = $headers
    }
    if ($null -ne $Body) { $parameters.Body = $Body | ConvertTo-Json -Depth 6 -Compress }
    Invoke-RestMethod @parameters
}

$customer = Call-Api POST '/api/auth/register' @{
    email = 'customer@example.com'
    password = 'Customer123!'
    name = 'Demo Customer'
    role = 'CUSTOMER'
}
$agent = Call-Api POST '/api/auth/register' @{
    email = 'agent@example.com'
    password = 'Agent123!'
    name = 'Demo Agent'
    role = 'AGENT'
}

$customerToken = (Call-Api POST '/api/auth/login' @{email='customer@example.com';password='Customer123!'}).token
$agentToken = (Call-Api POST '/api/auth/login' @{email='agent@example.com';password='Agent123!'}).token

$technical = Call-Api POST '/api/tickets' @{
    title = 'Cannot sign in after password reset'
    description = 'The reset link succeeded, but the new password is rejected on the login screen.'
    category = 'ACCOUNT'
    priority = 'HIGH'
} $customerToken
$billing = Call-Api POST '/api/tickets' @{
    title = 'Invoice total is incorrect'
    description = 'The August invoice includes a duplicate service charge.'
    category = 'BILLING'
    priority = 'URGENT'
} $customerToken

Call-Api PATCH "/api/tickets/$($technical.id)/assign" @{} $agentToken | Out-Null
Call-Api PATCH "/api/tickets/$($billing.id)/assign" @{} $agentToken | Out-Null
Call-Api POST "/api/tickets/$($technical.id)/comments" @{body='I can reproduce this in two browsers.'} $customerToken | Out-Null
Call-Api POST "/api/tickets/$($technical.id)/comments" @{body='Investigating the authentication logs now.'} $agentToken | Out-Null
Call-Api POST "/api/tickets/$($billing.id)/comments" @{body='The duplicate line is item 4 on the invoice.'} $customerToken | Out-Null
Call-Api PATCH "/api/tickets/$($technical.id)/status" @{status='IN_PROGRESS';note='Agent started investigation'} $agentToken | Out-Null
Call-Api PATCH "/api/tickets/$($billing.id)/status" @{status='IN_PROGRESS';note='Invoice review started'} $agentToken | Out-Null
Call-Api PATCH "/api/tickets/$($billing.id)/status" @{status='RESOLVED';note='Duplicate charge confirmed and credit issued'} $agentToken | Out-Null

Write-Output "Seeded customer $($customer.id), agent $($agent.id), and tickets $($technical.id), $($billing.id)."
