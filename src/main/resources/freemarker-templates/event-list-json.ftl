[
<#list list as item>
    {
        "id": "${item.id?c}",
        "event-id": "${item.eventId?c}"
    }<#sep>,</#sep>
</#list>
]
