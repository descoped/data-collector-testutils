{
    "id": "${item.id?c}",
    "event-id": "${item.eventId?c}"<#if records?has_content>,</#if>
    <#if records?has_content>
    "records": [
    <#list records as key, value>
        {"${key}": "${value}"}<#if key_has_next>,</#if>
    </#list>
    ]
    </#if>
}
