<#ftl output_format="XML" encoding="utf-8">
<?xml version="1.0" encoding="utf-8"?>
<#compress>
<entry>
    <id>${item.id?c}</id>
    <event-id>${item.eventId?c}</event-id>
    <#if records?has_content>
    <records>
    <#list records as key, value>
        <${key}>${value}</${key}>
    </#list>
    </records>
    </#if>
</entry>
</#compress>
