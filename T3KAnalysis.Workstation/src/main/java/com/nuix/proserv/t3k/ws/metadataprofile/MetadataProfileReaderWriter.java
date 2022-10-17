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
import java.util.Set;

/**
 * Marshall the metadata profile to and from disk.
 * <p>
 *     This class contains the methods needed to read the metadata profile XML and interpret it into a
 *     {@link MetadataProfile} object.  It also has the methods to do the reverse - write the MetadataProfile
 *     to disk.
 * </p>
 * <p>
 *     This class will only write to the {@link nuix.Case}'s Metadata storage location.
 * </p>
 * <p>
 *     <b>Implementation Note:</b> This class manually writes the XML and manually reads the XML and translate it to an
 *     object.  This is because JAXB, Jakarta, and the Apache XML marshallers don't function well with the CDATA
 *     content required by the ScriptedExpression.  Jackson would work, but it seems like nuix comes with a partial,
 *     and old, implementation of Jackson XML and I could get the dependencies required to make it work, nor could I
 *     get a newer version without the old version interfering.
 * </p>
 */
public class MetadataProfileReaderWriter {
    /**
     * Get the path to the metadata profile file from the current case's User Data stores.
     * <p>
     *     This method will get the file path for the specified profile in the current case directory, but does not
     *     check or ensure that the file exists.
     * </p>
     * @param currentCase The {@link nuix.Case} in which the profile will stored
     * @param profileName The name (without a file extension) for the profile
     * @return A String with the full/absolute path to the metadata profile file as it would exist in the current case's user data store.
     */
    protected String getMetadataProfilePath(Case currentCase, String profileName) {
        String metadataProfilePath = currentCase.getLocation().getAbsolutePath() + File.separator +
                "Stores" + File.separator + "User Data" + File.separator +
                "Metadata Profiles" + File.separator + profileName + ".profile";
        File metadataProfileFile = new File(metadataProfilePath);

        return metadataProfileFile.getAbsolutePath();
    }

    /**
     * Read the metadata profile into a {@link MetadataProfile} object.
     * <p>
     *     If the requested profile does not exist in the provided {@link nuix.Case}, then an empty MetadataProfile
     *     will be returned.  Otherwise, the file will be read and the results returned.
     * </p>
     * @param currentCase The {@link nuix.Case} in which the metadata profile should be stored
     * @param profileName A string with the name of the profile to load.
     * @return A {@link MetadataProfile} object containing the metadata profile, or default values if the profile doesn't exist in the case.
     * @throws T3KApiException if the file exists but can't be accessed.
     */
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
                    MetadataType type = MetadataType.valueOf(metadataType);

                    NodeList expressions = ((Element)metadataColumn).getElementsByTagName("scripted-expression");
                    if (expressions.getLength() > 0) {
                        Element scriptedExpression = (Element)expressions.item(0);
                        String scriptType = scriptedExpression.getElementsByTagName("type").item(0).getTextContent();
                        ScriptType language = ScriptType.valueOf(scriptType);
                        String script = scriptedExpression.getElementsByTagName("script").item(0).getTextContent().trim();

                        ScriptedExpression expression = new ScriptedExpression(language, script);
                        Metadata column = new Metadata(type, name, expression);
                        columns.add(column);
                    } else {
                        Metadata column = new Metadata(type, name, null);
                        columns.add(column);
                    }
                }
            }

            return profile;

        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new T3KApiException("Error reading the T3K Metadata Profile", e);
        }
    }

    /**
     * Store the metadata profile to disk.
     * <p>
     *     The provided {@link MetadataProfile} will be stored in the current {@link nuix.Case}'s User Data store with
     *     the provided name.  This method will write the XML for the metadata, replacing the contents of the file
     *     which may (or may not) already exist.
     * </p>
     * @param profile The {@link MetadataProfile} to store on disk.  It must not be null.
     * @param currentCase The {@link nuix.Case} to store the profile in.
     * @param profileName The name of the metadata profile (without the .profile extension).  If the profile exists it
     *                    will be overwritten.
     * @throws T3KApiException if the file can't be written.
     */
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

    /**
     * Add a column to the metadata profile XML
     * @param doc The {@link Document} used to hold the XML DOM while buiding the profile
     * @param parent The parent {@link Element} the column's XML element tree will be added to
     * @param column The {@link Metadata} object which holds details about the column to add to the XML
     */
    private void createColumn(Document doc, Element parent, Metadata column) {
        Element col = doc.createElement("metadata");
        col.setAttribute("name", column.getName());
        col.setAttribute("type", column.getType().name());
        parent.appendChild(col);

        ScriptedExpression expression = column.getScriptedExpression();
        if(null != expression) {
            Element scriptExpression = doc.createElement("scripted-expression");
            col.appendChild(scriptExpression);
            Element scriptType = doc.createElement("type");
            scriptType.appendChild(doc.createTextNode(expression.getType().name()));
            scriptExpression.appendChild(scriptType);

            Element script = doc.createElement("script");
            script.appendChild(doc.createCDATASection(expression.getScript().trim()));
            scriptExpression.appendChild(script);
        }
    }
}
