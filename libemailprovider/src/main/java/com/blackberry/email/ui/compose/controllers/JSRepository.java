package com.blackberry.email.ui.compose.controllers;


public class JSRepository {

    public static final String SETUP_SCRIPT_ENVIRONMENT = 
            " var _cachedNodes = {};                                          "
          + " function getNode(nodeName) {                                    "
          + "  if (_cachedNodes.nodeName == undefined) {                      "
          + "      _cachedNodes[nodeName] = document.getElementById(nodeName);"
          + "      }                                                          "
          + "  return _cachedNodes[nodeName];                                 "
          + " }"

          + " function deleteNode(nodeRef) {                                  "
          + "   if (nodeRef)  {                                               "
          + "       nodeRef.parentNode.removeChild(nodeRef);                  "
          + "   }                                                             "
          + " }";

    public static final String RESPONSE_SECTION =
            " <div name=\"BB10\" id=\"BB10_response_div\" dir=\"auto\"                                                                                         "
          + "      style=\"width:100%;background:&quot;#ffffff&quot;; font-size: initial;font-family:&quot;Calibri&quot;,&quot;Slate Pro&quot;,sans-serif,&quot;sans-serif&quot;;color:#1f497d;\">"
          + " <br style=\"display:initial\"></div>                                                                                                                                     "
          + " <div name=\"BB10\" id=\"response_div_spacer\" dir=\"auto\"                                                                                       "
          + "      style=\"width:100%;background:&quot;#ffffff&quot;; font-size: initial;font-family:&quot;Calibri&quot;,&quot;Slate Pro&quot;,sans-serif,&quot;sans-serif&quot;;color:#1f497d;\">"
          + " <br style=\"display:initial\"></div>                                                                                                                                     "
          + " <div id=\"_signaturePlaceholder\" name=\"BB10\" dir=\"auto\"                                                                                     "
          + "    style=\"font-size: initial;font-family:&quot;Calibri&quot;,&quot;Slate Pro&quot;,sans-serif,&quot;sans-serif&quot;;color:#1f497d;\"></div>                                       ";

    public static final String ORIGINAL_MSG_SEPARATOR_BEGIN =
              "<div id=\"_bb10TempSeparator\" contenteditable=\"false\">"
            + " <table width=\"100%\" style=\"background-color:white; border-spacing:0px;\">                                    "
            + "    <tr><td></td><td id=\"_separatorInternal\" rowspan=2 style=\"text-align:center\">                                                                                  "
            + "        <span id=\"_bb10TempSeparatorText\" style=\"background-color:white; color:#0073BC;font-size:smaller;font-family:&quot;Slate Pro&quot;\">&nbsp; Original Message &nbsp;</span>"
            + "    </td></tr>                                                                                                                                                         "
            + "    <tr> <td colspan=2><div style=\"border:none;border-top:solid #0073BC 1.0pt;\"></div>                                                                               "
            + "    </td></tr>"
            + "    <tr><td colspan='1'></td><td id='_showDetails' align='right'><span style=' background-color:white; color:#0073BC;font-size:smaller;font-family:&quot;Slate Pro&quot;'>Show Details</span></td></tr>"
            + " </table></div>                                                                                                                                           "
            + "<table id=\'_pHCWrapper\' width=\"100%\" style=\"background-color:white;border-spacing:0px; display: none;\"> <tr><td id=\"_persistentHeaderContainer\" colspan=2>                      "
            + "     <div id=\"_persistentHeader\" style=\"font-size: smaller;font-family:&quot;Tahoma&quot;,&quot;BB Alpha Sans&quot;,&quot;Slate Pro&quot;,sans-serif,&quot;sans-serif&quot;;\">  ";

    public static final String ORIGINAL_MSG_SEPARATOR_END = 
              "</div></td></tr></table><div id=\"_persistentHeaderEnd\" style=\"border:none;border-top:solid #babcd1 1pt; display: none;\"></div>"
            + "<br><div name=\"BB10\" dir=\"auto\" id=\"_originalContent\">";

    public static final String ORIGINAL_MSG_CONTENT_END = "<!--end of _originalContent --></div>";

    public static final String INSERT_AUTO_SIGNATURE = 
              " function setAutoSignature(signatureText) {                                     "
            + "     var signaturePlaceholder = getNode('_signaturePlaceholder');               "
            + "     if (signaturePlaceholder) {                                                "
            + "         signaturePlaceholder.innerHTML = signatureText;                        "
            + "     } else {                                                                   "
            + "         console.log(\"JavaScriptRepository:: Missing signature placeholder.\");"
            + "     }                                                                          "
            + " } setAutoSignature('%s');                                                      ";

    public static final String ATTACH_SHOW_MSG_DETAILS_TOGGLE =
              " (function() {                                                    "
            + " var wrapper = getNode('_pHCWrapper');                            "
            + " var separator = getNode('_persistentHeaderEnd');                 "
            + " var showDetails = getNode('_showDetails');                       "
            + " if (showDetails) {                                               "
            + "      showDetails.addEventListener('touchstart',function(event) { "
            + "      event.preventDefault();                                     "
            + "      wrapper.style.display = 'table';                            "
            + "      separator.style.display = 'block';                          "
            + "      showDetails.style.display = 'none';                         "
            + "     });                                                          "
            + " }                                                                "
            + " })();                                                            "; 

    public static final String LTR_HEADER_FIELD_TEMPLATE = "<div id='%s'><b>%s</b>%s</div>";

    public static final String CLEANUP_BASED_ON_INTERNAL_RULES =
              " (function() {                                                     "
            + "  var persistentHeader = getNode(\"_persistentHeader\");           "
            + "  var separator = getNode(\"_bb10TempSeparator\");                 "
            + "  if (separator && persistentHeader) {                             "
            + "      persistentHeader.removeAttribute(\'id\');                    "
            + "      persistentHeader.removeAttribute(\'style\');                 "
            + "      persistentHeader.setAttribute(\'style\',\'border:none;border-top:solid #b5c4df 1.0pt;padding:3.0pt 0in 0in 0in;\');"
            + "      persistentHeader.style.fontFamily = \'Tahoma,BB Alpha Sans,Slate Pro\'; "
            + "      persistentHeader.style.fontSize = \'10pt\';                  "
            + "  } else {"
            + "     var persistentHeaderEnd = getNode(\"_persistentHeaderEnd\");  "
            + "     if (!persistentHeader && persistentHeaderEnd) { deleteNode(persistentHeaderEnd); }"
            + "     console.log(\"JavaScriptRepository:: Could not add persistent separator - either user deleted it, or this was a fresh or custom compose.\");"
            + "  }"
            + "  deleteNode(separator);                                           "
            + "  _cssExclusionList.push('_pHCWrapper');                           "
            + "  _cssExclusionList.push('_originalContent');                      "
            + "  var numElements = _cssExclusionList.length;                      "
            + "  for (var index=0; index < numElements; index++) {                "
            + "       var nodeToCleanup = getNode(_cssExclusionList[index]);      "
            + "       if (nodeToCleanup) { nodeToCleanup.removeAttribute('id'); } "
            + "  }                                                                "
            + "})();";

    /**
     * Cascading style sheets get applied to the document in the following order:
     *    a. User Agent's (webkit's),
     *    b. followed by author's (or internal) - style sheets from the original message in our case,
     *    c. followed by external - we don't care about these, our filter + webkit's "same origin policy" will block any request(s) to download external stylesheets anyway.
     *    d. followed by inline - this is the *only* way we can restrict/control style sheets from our side. (without using scoped stylesheets - HTML5 or iFrames/object elements)
     * At each step, webkit will prefer the later if there is a conflict on a particular style.
     * So, for the elements that we build, this script attempts to revert *some* styles to what webkit provides in it's default style sheet. (step #a)
     * Test cases: linkedin , feedbliz, sears, groupon, 8px body margin.
     */
    public static final String RESTRICT_DOCUMENT_WIDE_STYLES_TO_ORIGINAL_CONTENT = 
            " (function() {  "
          + "_cssExclusionList = [ 'BB10_response_div', 'response_div_spacer', '_signaturePlaceholder','_bb10TempSeparator',"
          + "                          '_separatorInternal','_persistentHeaderContainer', '_persistentHeaderEnd'];"
          + " var originalDocumentColor = document.body.bgColor; "
          + " var docWideCSSIfPresent = document.body.style;"
          + " var originalContent = getNode('_originalContent');"
          + " if (originalContent) {"
          + "     document.body.removeAttribute(\'style\');"
          + "     document.body.style.backgroundColor = '#ffffff';"
          + "     document.body.style.backgroundImage = \'initial\';"
//        + "     document.body.style.margin = '8px'; "                                                 /**  8px = default value used by UA, for margins               */
          + "     document.body.style.lineHeight = \'initial\';"                                        /**  Falling back to initial-value doesn't help in case of     */
          + "     originalContent.style = docWideCSSIfPresent.cssText + originalContent.style.cssText;" /**  margins because CSS specs define default margins to be 0. */
          + "     originalContent.style.backgroundColor = originalDocumentColor;"
          + "     var numElements = _cssExclusionList.length; "
          + "     for (var index=0; index < numElements; index++) {"
          + "          var responseElement = getNode(_cssExclusionList[index]);"
          + "          if (responseElement) {"
//        + "              responseElement.style.margin = \'initial\';"
          + "              responseElement.style.padding = \'initial\';"
//        + "              responseElement.style.display = 'initial';"
          + "              responseElement.style.fontSize = \'initial\';"
          + "              if (responseElement.id != '_separatorInternal') {"
          + "                  responseElement.style.textAlign = \'initial\';"
          + "              }"
          + "              responseElement.style.backgroundColor = '#ffffff';"
          + "          }"
          + "     }"
          + " } else if (document.body.style.cssText != \'\') {"
          + "     console.log(\'JavaScriptRepository [Error] - Could not reset document wide stylesheets.\');"
          +" };})();";
}
