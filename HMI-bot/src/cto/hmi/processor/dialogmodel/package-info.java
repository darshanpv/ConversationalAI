@XmlSchema(
	    namespace = "http://cto.net/hmi/"+cto.hmi.model.definition.DialogModel.VERSION, 
	    xmlns={@XmlNs(namespaceURI = "http://cto.net/hmi/"+cto.hmi.model.definition.DialogModel.VERSION, prefix = "n")},
	    elementFormDefault = XmlNsForm.UNQUALIFIED,
	    attributeFormDefault = XmlNsForm.UNQUALIFIED
		)
package cto.hmi.processor.dialogmodel;

import javax.xml.bind.annotation.*;