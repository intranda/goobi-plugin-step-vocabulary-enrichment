package de.intranda.goobi.plugins;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.SwapException;
import io.goobi.vocabulary.exchange.FieldDefinition;
import io.goobi.vocabulary.exchange.FieldInstance;
import io.goobi.vocabulary.exchange.FieldValue;
import io.goobi.vocabulary.exchange.TranslationInstance;
import io.goobi.vocabulary.exchange.Vocabulary;
import io.goobi.vocabulary.exchange.VocabularyRecord;
import io.goobi.vocabulary.exchange.VocabularySchema;
import io.goobi.workflow.api.vocabulary.VocabularyAPIManager;
import io.goobi.workflow.api.vocabulary.jsfwrapper.JSFVocabularyRecord;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@PluginImplementation
@Log4j2
public class VocabularyEnrichmentStepPlugin implements IStepPluginVersion2 {

    @Getter
    private String title = "intranda_step_vocabulary_enrichment";
    @Getter
    private Step step;
    @Getter
    private String value;
    @Getter
    private boolean allowTaskFinishButtons;
    private String returnPath;

    private SubnodeConfiguration myconfig;
    private ArrayList<VocabItem> vocabItems;
    VariableReplacer replacer;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;

        // read parameters from correct block in configuration file
        myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
        allowTaskFinishButtons = myconfig.getBoolean("allowTaskFinishButtons", false);

        // setup
        List<HierarchicalConfiguration> itemList = myconfig.configurationsAt("item");

        vocabItems = new ArrayList<>();
        for (HierarchicalConfiguration sub : itemList) {
            vocabItems.add(new VocabItem(sub));
        }

        log.info("VocabularyEnrichment step plugin initialized");
    }

    @Override
    public PluginReturnValue run() {
        try {

            // read mets file
            Fileformat ff = step.getProzess().readMetadataFile();
            Prefs prefs = step.getProzess().getRegelsatz().getPreferences();

            replacer = new VariableReplacer(ff.getDigitalDocument(), prefs, step.getProzess(), step);

            for (VocabItem item : vocabItems) {

                checkMetadata(prefs, item);
            }

        } catch (ReadException | PreferencesException | IOException | SwapException e) {
            log.error(e);
        }

        return PluginReturnValue.FINISH;
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return null;
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    private void checkMetadata(Prefs prefs, VocabItem item) {
        VocabularyAPIManager api = VocabularyAPIManager.getInstance();
        Vocabulary vocabulary = api.vocabularies().findByName(item.getVocab());
        VocabularySchema schema = api.vocabularySchemas().get(vocabulary.getSchemaId());
        Optional<FieldDefinition> searchField = schema.getDefinitions().stream()
                .filter(d -> d.getName().equals(item.getTarget()))
                .findFirst();

        if (searchField.isEmpty()) {
            Helper.setFehlerMeldung("Field " + item.getTarget() + " not found in vocabulary " + vocabulary.getName());
            return;
        }

        String metaName = item.getSource(); //looks like meta.color
        String strValue = metaName.replace("meta.", "metas."); //want multiple answers:

        strValue = replacer.replace("{" + strValue + "}");
        ArrayList<String> lstValues = getValues(strValue);

        for (String value : lstValues) {
            // TODO: This will not work consistently for main values with translations
            Optional<JSFVocabularyRecord> hit = api.vocabularyRecords().search(vocabulary.getId(), searchField.get().getId() + ":" + value)
                    .getContent()
                    .stream()
                    .filter(r -> r.getMainValue().equals(value))
                    .findFirst();

            //aready exists: then ok
            if (hit.isEmpty()) {
                api.vocabularyRecords().create(makeNewRecord(vocabulary, schema, item, value));
            }
        }
    }

    //extract the entries: they appear separated by commas with no spaces
    private ArrayList<String> getValues(String strValue) {

        ArrayList<String> lstStrings = new ArrayList<>();
        String strEntry = "";

        for (int i = 0; i < strValue.length(); i++) {

            //finished?
            if (i == strValue.length() - 1) {
                strEntry += strValue.charAt(i);
                lstStrings.add(strEntry);
                break;
            }

            //comma?
            if (i > 0 && strValue.charAt(i - 1) != ' ' && strValue.charAt(i) == ',' && strValue.charAt(i + 1) != ' ') {
                lstStrings.add(strEntry);
                strEntry = "";
                continue;
            }

            strEntry += strValue.charAt(i);
        }

        return lstStrings;
    }

    // TODO
    private VocabularyRecord makeNewRecord(Vocabulary vocabulary, VocabularySchema schema, VocabItem item, String value) {
        HashMap<String, String> mapFields = item.getLstFieldsToGenerate();

        VocabularyRecord result = new VocabularyRecord();
        result.setVocabularyId(vocabulary.getId());
        result.setFields(new HashSet<>());

        for (FieldDefinition definition : schema.getDefinitions()) {
            String definitionName = definition.getName();
            FieldInstance field = new FieldInstance();
            field.setDefinitionId(definition.getId());
            String fieldValue = "-";

            //main field:
            if (definitionName.contentEquals(item.getTarget())) {
                fieldValue = value;
            }

            //fields to generate:
            else if (mapFields.containsKey(definitionName)) {
                fieldValue = mapFields.get(definitionName);
            }

            TranslationInstance translation = new TranslationInstance();
            translation.setValue(fieldValue);
            FieldValue fv = new FieldValue();
            fv.setTranslations(List.of(translation));
            field.setValues(List.of(fv));
            result.getFields().add(field);
        }

        return result;
    }

    @Data
    private class VocabItem {

        private String vocab;
        private String source;
        private String target;
        private HashMap<String, String> lstFieldsToGenerate;

        public VocabItem(HierarchicalConfiguration sub) {

            vocab = sub.getString("vocabulary");
            source = sub.getString("source");
            target = sub.getString("target");
            lstFieldsToGenerate = new HashMap<>();

            List<HierarchicalConfiguration> gens = sub.configurationsAt("//generate");

            if (gens != null) {
                for (HierarchicalConfiguration gen : gens) {
                    String strField = (String) gen.getRootNode().getAttributes("field").get(0).getValue();
                    String strContent = (String) gen.getRootNode().getAttributes("content").get(0).getValue();
                    lstFieldsToGenerate.put(strField, strContent);
                }
            }
        }
    }
}
