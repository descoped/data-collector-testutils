<#ftl output_format="XML" encoding="utf-8">
<?xml version="1.0" encoding="utf-8"?>
<#compress>
<feed xmlns="http://www.w3.org/2005/Atom">
    <title>Mock Atom Feed</title>
    <#if linkPreviousURL?has_content>
    <link rel="previous" href="${linkPreviousURL}" />
    </#if>
    <#if linkSelfURL?has_content>
    <link rel="self" href="${linkSelfURL}" />
    </#if>
    <#if linkNextURL?has_content>
    <link rel="next" href="${linkNextURL}" />
    </#if>
    <updated>${.now?iso_utc}</updated>
    <author>
        <name>Mock Atom Feed</name>
    </author>
    <id>urn:uuid:${fromPosition}</id>

    <#list entries as entry>
    <entry>
        <title>Atom feed message #${entry.eventId}</title>
        <link href="http://example.org/2003/12/13/atom03"/>
        <id>urn:uuid:${entry.eventId}</id>
        <updated>${.now?iso_utc}</updated>
        <summary>Message document</summary>
        <content type="application/xml" />
    </entry>
    </#list>
</feed>
</#compress>
