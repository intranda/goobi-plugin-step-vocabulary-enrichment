package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.junit.Test;

import io.goobi.vocabulary.exchange.FieldDefinition;
import io.goobi.vocabulary.exchange.FieldInstance;
import io.goobi.vocabulary.exchange.FieldValue;
import io.goobi.vocabulary.exchange.Vocabulary;
import io.goobi.vocabulary.exchange.VocabularyRecord;
import io.goobi.vocabulary.exchange.VocabularySchema;

public class VocabularyEnrichmentPluginTest {

    private static final String ITEM = "<config><item>"
            + "<source>meta.originalAccessPersons</source>"
            + "<vocabulary>Access points</vocabulary>"
            + "<target>original value</target>"
            + "<generate field=\"type\" content=\"AccessPersons\" />"
            + "</item></config>";

    private VocabularyRecord newRecord() throws ConfigurationException {
        XMLConfiguration config = new XMLConfiguration();
        config.setExpressionEngine(new XPathExpressionEngine());
        config.load(new StringReader(ITEM));
        HierarchicalConfiguration itemConfig = config.configurationsAt("item").get(0);

        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setId(1L);
        VocabularySchema schema = new VocabularySchema();
        schema.setDefinitions(List.of(definition(10L, "original value"), definition(11L, "type"), definition(12L, "authority")));

        VocabularyEnrichmentStepPlugin plugin = new VocabularyEnrichmentStepPlugin();
        return plugin.makeNewRecord(vocabulary, schema, plugin.new VocabItem(itemConfig), "Jane Doe");
    }

    private static FieldDefinition definition(long id, String name) {
        FieldDefinition definition = new FieldDefinition();
        definition.setId(id);
        definition.setName(name);
        return definition;
    }

    @Test
    public void newRecordCanBeCleanedUpInPlaceBySave() throws ConfigurationException {
        // VocabularyRecordAPI.save() removes empty entries in place, so all collections must be modifiable
        VocabularyRecord vocabularyRecord = newRecord();
        for (FieldInstance field : vocabularyRecord.getFields()) {
            for (FieldValue value : field.getValues()) {
                value.getTranslations().removeIf(t -> t.getValue().isEmpty());
            }
            field.getValues().removeIf(v -> v.getTranslations().isEmpty());
        }
        vocabularyRecord.getFields().removeIf(f -> f.getValues().isEmpty());

        assertEquals(3, vocabularyRecord.getFields().size());
    }
}
