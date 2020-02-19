<#ftl output_format="XML" encoding="utf-8">
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<#compress>
<feed>
    <#if linkNextURL?has_content && list?has_content>
    <link rel="next" href="${linkNextURL}" />
    </#if>
    <#list list as item>
    <entry>
        <id>${item.id}</id>
        <event-id>${item.eventId}</event-id>
    </entry>
    </#list>
</feed>
</#compress>
