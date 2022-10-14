package com.nuix.proserv.t3k.ws.metadataprofile;

import nuix.Case;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.Assert.*;

public class MetadataProfileTests {
    private static final String PROFILE_RELATIVE_PATH = "build\\resources\\test\\com\\nuix\\proserv\\t3k\\ws\\metadataprofile";

    @Test
    public void BasicProfilesLoad() {
        MetadataProfileReaderWriter profileSource = new MetadataProfileReaderWriter() {
            @Override protected String getMetadataProfilePath(Case caseName, String profileName) {
                return Path.of(PROFILE_RELATIVE_PATH, profileName + ".profile").toAbsolutePath().toString();
            }
        };

        // The mock reader/writer is going to read from fixed position, don't need case info
        MetadataProfile mdp = profileSource.readProfile(null, "Basic");

        assertNotNull(mdp);
        assertNotNull(mdp.getColumns());
        assertEquals(3, mdp.getColumns().size());
        Metadata column = mdp.getColumns().stream().findFirst().get();
        assertEquals("SPECIAL", column.getType());
        assertEquals("Name", column.getName());
    }

    private String removeWhitespace(String source) {
        return source.replaceAll("\\s+", "");
    }

    @Test
    public void BasicProfileWrite() {
        MetadataProfile mdp = new MetadataProfile();
        Set<Metadata> columns = mdp.getColumns();
        Metadata column = new Metadata("SPECIAL", "Name", null);
        Metadata column2 = new Metadata("CUSTOM", "T3K Detections", null);
        Metadata column3 = new Metadata("CUSTOM", "T3K Detections|Count", null);
        columns.add(column);
        columns.add(column2);
        columns.add(column3);

        MetadataProfileReaderWriter profileSource = new MetadataProfileReaderWriter() {
            @Override protected String getMetadataProfilePath(Case caseName, String profileName) {
                return Path.of(profileName + ".profile").toAbsolutePath().toString();
            }
        };

        profileSource.writeProfile(mdp, null, "output");

        MetadataProfile mdp2 = profileSource.readProfile(null, "output");
        assertEquals(mdp, mdp2); // Written profiles can be read and produce same profile
    }

    @Test
    public void ScriptedProfilesLoad() {
        MetadataProfileReaderWriter profileSource = new MetadataProfileReaderWriter() {
            @Override protected String getMetadataProfilePath(Case caseName, String profileName) {
                return Path.of(PROFILE_RELATIVE_PATH, profileName + ".profile").toAbsolutePath().toString();
            }
        };

        MetadataProfile mdp = profileSource.readProfile(null, "Scripted");


            assertNotNull(mdp);
            assertNotNull(mdp.getColumns());
            assertEquals(1, mdp.getColumns().size());
            Metadata column = mdp.getColumns().stream().findFirst().get();
            assertEquals("SPECIAL", column.getType());
            assertEquals("Army Tank", column.getName());
            ScriptedExpression expression = column.getScriptedExpression();
            assertNotNull(expression);
            assertEquals("ruby", expression.getType());
            assertEquals("java_import \"com.nuix.proserv.t3k.ws.MetadataProfileBase\"\n" +
                    "MetadataProfileBase::display_object_data $current_item.custom_metadata, \"army_tank\"",
                    expression.getScript());
    }

    @Test
    public void ScriptedProfileWrite() {
        String outputPath = "scripted.profile";

        MetadataProfile mdp = new MetadataProfile();
        Set<Metadata> columns = mdp.getColumns();
        ScriptedExpression expression = new ScriptedExpression("ruby",
                "java_import \"com.nuix.proserv.t3k.ws.MetadataProfileBase\"\n" +
                "MetadataProfileBase::display_object_data $current_item.custom_metadata, \"army_tank\"");

        System.out.println(expression);

        Metadata column = new Metadata("SPECIAL", "Army Tank", expression);
        columns.add(column);

        MetadataProfileReaderWriter profileSource = new MetadataProfileReaderWriter() {
            @Override protected String getMetadataProfilePath(Case caseName, String profileName) {
                return Path.of(profileName + ".profile").toAbsolutePath().toString();
            }
        };

        profileSource.writeProfile(mdp, null, "scripted_output");

        MetadataProfile mdp2 = profileSource.readProfile(null, "scripted_output");
        assertEquals(mdp, mdp2);
    }
}
