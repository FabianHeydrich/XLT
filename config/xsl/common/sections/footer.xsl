<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:template name="footer">
	<xsl:param name="productUrl" select="/testreport/configuration/version/productURL" />
    <xsl:param name="productName" select="/testreport/configuration/version/productName" />
    <xsl:param name="productVersion" select="/testreport/configuration/version/version" />
    <footer>
        Copyright (c) 2005-2023 <a href="http://www.xceptance.com/" title="Visit the Xceptance website">Xceptance Software Technologies GmbH</a><br />
        Created with
                <a href="{$productUrl}?source=TestReport">
                    <span class="productname">
                        <xsl:value-of select="$productName" />
                    </span>
                    <span class="productversion">
                        <xsl:value-of select="$productVersion" />
                    </span>
                </a>
    </footer>
</xsl:template>

</xsl:stylesheet>
