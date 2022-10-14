package com.nuix.proserv.t3k.ws.metadataprofile;

import com.nuix.proserv.t3k.T3KApiException;
import nuix.Case;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class MetadataProfileReaderWriter {
    protected String getMetadataProfilePath(Case currentCase, String profileName) {
        String metadataProfilePath = currentCase.getLocation().getAbsolutePath() + File.separator +
                "Stores" + File.separator + "User Data" + File.separator +
                "Metadata Profiles" + File.separator + profileName + ".profile";
        File metadataProfileFile = new File(metadataProfilePath);

        return metadataProfileFile.getAbsolutePath();
    }

    public MetadataProfile readProfile(Case currentCase, String profileName) {
        String metadataProfilePath = getMetadataProfilePath(currentCase, profileName);
        MetadataProfile profile = new MetadataProfile();

        if (Files.notExists(Path.of(metadataProfilePath))) {
            return profile;
        }

        try {
            DocumentBuilder profileBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document metadataProfile = profileBuilder.parse(metadataProfilePath);
            metadataProfile.getDocumentElement().normalize();

            NodeList metadataColumns = metadataProfile.getElementsByTagName("metadata");
            int numberOfColumns = metadataColumns.getLength();

            if (0 == numberOfColumns) {
                return profile;
            }

            Set<Metadata> columns = profile.getColumns();

            for(int columnIndex = 0; columnIndex < numberOfColumns; columnIndex++) {
                Node metadataColumn = metadataColumns.item(columnIndex);

                NamedNodeMap metadataColumnDetails = metadataColumn.getAttributes();
                if(null != metadataColumnDetails) {
                    String name = metadataColumnDetails.getNamedItem("name").getNodeValue();
                    String metadataType = metadataColumnDetails.getNamedItem("type").getNodeValue();

                    NodeList expressions = ((Element)metadataColumn).getElementsByTagName("scripted-expression");
                    if (expressions.getLength() > 0) {
                        Element scriptedExpression = (Element)expressions.item(0);
                        String scriptType = scriptedExpression.getElementsByTagName("type").item(0).getTextContent();
                        String script = scriptedExpression.getElementsByTagName("script").item(0).getTextContent().trim();

                        ScriptedExpression expression = new ScriptedExpression(scriptType, script);
                        Metadata column = new Metadata(metadataType, name, expression);
                        columns.add(column);
                    } else {
                        Metadata column = new Metadata(metadataType, name, null);
                        columns.add(column);
                    }
                }
            }

            return profile;

        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new T3KApiException("Error reading the T3K Metadata Profile", e);
        }
    }

    public void writeProfile(MetadataProfile profile, Case currentCase, String profileName) {
        try {
            DocumentBuilder profileBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document metadataProfile = profileBuilder.newDocument();
            Element profileElement = metadataProfile.createElementNS("http://nuix.com/fbi/metadata-profile",
                    "metadata-profile");
            metadataProfile.appendChild(profileElement);
            Element metadateList = metadataProfile.createElement("metadata-list");
            profileElement.appendChild(metadateList);

            for(Metadata column : profile.getColumns()) {
                createColumn(metadataProfile, metadateList, column);
            }

            String profilePath = getMetadataProfilePath(currentCase, profileName);

            metadataProfile.setXmlStandalone(true);
            DOMSource metadataProfileDOM = new DOMSource(metadataProfile);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StreamResult outputResult = new StreamResult(profilePath);
            transformer.transform(metadataProfileDOM, outputResult);

            System.out.println("Finished output to " + profilePath);
        } catch (ParserConfigurationException|TransformerException e) {
            throw new T3KApiException("Error writing the Metadata Profile to disk.", e);
        }
    }

    private void createColumn(Document doc, Element parent, Metadata column) {
        Element col = doc.createElement("metadata");
        col.setAttribute("name", column.getName());
        col.setAttribute("type", column.getType());
        parent.appendChild(col);

        ScriptedExpression expression = column.getScriptedExpression();
        if(null != expression) {
            Element scriptExpression = doc.createElement("scripted-expression");
            col.appendChild(scriptExpression);
            Element scriptType = doc.createElement("type");
            scriptType.appendChild(doc.createTextNode(expression.getType()));
            scriptExpression.appendChild(scriptType);

            Element script = doc.createElement("script");
            script.appendChild(doc.createCDATASection(expression.getScript().trim()));
            scriptExpression.appendChild(script);
        }
    }
}
