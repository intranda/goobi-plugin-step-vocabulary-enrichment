package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.request.CoreApiMapping.Meta;
import org.goobi.beans.Step;
import org.goobi.managedbeans.VocabularyBean;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;
import org.goobi.vocabulary.Definition;
import org.goobi.vocabulary.Field;
import org.goobi.vocabulary.VocabRecord;
import org.goobi.vocabulary.Vocabulary;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.VocabularyManager;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.WriteException;

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

        vocabItems = new ArrayList<VocabItem>();
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

        } catch (ReadException | PreferencesException | WriteException | IOException | InterruptedException | SwapException | DAOException e) {
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

        VocabularyBean vocabBean = new VocabularyBean();
        Vocabulary vocab = VocabularyManager.getVocabularyByTitle(item.getVocab());

        //only if the vocabulary is defined:
        if (vocab == null) {
            return;
        }

        vocabBean.setCurrentVocabulary(vocab);
        vocabBean.editVocabulary();

        String metaName = item.getSource(); //looks like meta.color
        String strValue = metaName.replace("meta.", "metas."); //want multiple answers:

        strValue = replacer.replace("{" + strValue + "}");
        ArrayList<String> lstValues = getValues(strValue);

        Boolean boChange = false;

        for (String value : lstValues) {

            List<VocabRecord> lstRecords = VocabularyManager.findRecords(item.getVocab(), value, item.getTarget());

            //aready exists: then ok
            if (!lstRecords.isEmpty()) {
                continue;
            }

            //otherwise create a new record:
            VocabRecord record = makeNewRecord(vocabBean.getCurrentVocabulary(), item, value);
            vocabBean.setCurrentVocabRecord(record);
            vocabBean.saveRecordEdition();
            boChange = true;
        }

        if (boChange) {
            vocabBean.saveVocabulary();
        }
    }

    //extract the entries: they appear separated by commas with no spaces
    private ArrayList<String> getValues(String strValue) {

        ArrayList<String>  lstStrings = new ArrayList<String>();
        String strEntry = "";
        
        for (int i = 0; i < strValue.length(); i++) {
            
            //finished?
            if (i == strValue.length()-1) {
                strEntry += strValue.charAt(i);
                lstStrings.add(strEntry);
                break;
            }
            
            //comma?
            if (i > 0 && strValue.charAt(i-1) != ' ' && strValue.charAt(i) == ',' && strValue.charAt(i+1) != ' ') {
                lstStrings.add(strEntry);
                strEntry = "";
                continue;
            } 
            
            strEntry += strValue.charAt(i);
        }
        
        return lstStrings;
    }

    private VocabRecord makeNewRecord(Vocabulary vocab, VocabItem item, String value) {

        HashMap<String, String> mapFields = item.getLstFieldsToGenerate();
        ArrayList<Field> fields = new ArrayList<Field>();

        List<Definition> lstDefs = vocab.getStruct();

        for (Definition def : lstDefs) {

            String strLabel = def.getLabel();
            Field field = new Field(strLabel, "-", "", def);

            //main field:
            if (strLabel.contentEquals(item.getTarget())) {
                field = new Field(strLabel, "-", value, def);
            }

            //fields to generate:
            if (mapFields.containsKey(strLabel)) {
                field = new Field(strLabel, "-", mapFields.get(strLabel), def);
            }

            fields.add(field);
        }

        VocabRecord newRecord = new VocabRecord(null, vocab.getId(), fields);

        return newRecord;
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
            lstFieldsToGenerate = new HashMap<String, String>();

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
