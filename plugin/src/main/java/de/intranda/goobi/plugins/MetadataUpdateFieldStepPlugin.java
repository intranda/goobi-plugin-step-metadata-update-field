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
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.UghHelper;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.helper.exceptions.UghHelperException;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.WriteException;

@PluginImplementation
@Log4j2
public class MetadataUpdateFieldStepPlugin implements IStepPluginVersion2 {
    
    @Getter
    private String title = "intranda_step_metadata_update_field";
    @Getter
    private Step step;
    private String returnPath;
    private List<HierarchicalConfiguration> updates;
    
    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;
        
        SubnodeConfiguration config = ConfigPlugins.getProjectAndStepConfig(title, step);
        updates = config.configurationsAt("./update");
        
        log.info("Metadata-update-field step plugin initialized");
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return "/uii/plugin_step_metadata-update-field.xhtml";
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

    @Override
    public PluginReturnValue run() {
        boolean successfull = true;
        
        try {
            UghHelper ughhelp = new UghHelper();
            boolean updated = false;
            Process process = step.getProzess();
            Prefs prefs = process.getRegelsatz().getPreferences();
            Fileformat fileformat = process.readMetadataFile();
            VariableReplacer replacer = new VariableReplacer(fileformat != null ? fileformat.getDigitalDocument() : null,
                    prefs, process, step);
            
            for (HierarchicalConfiguration myconfig : updates) {
                // read parameters from correct block in configuration file
                boolean forceUpdate = myconfig.getBoolean("forceUpdate", false);
                String field = myconfig.getString("field", null);
                List<String> elements = Arrays.asList(myconfig.getStringArray("element"));
                List<ParameterItem> parameterList = new ArrayList<>();
                List<HierarchicalConfiguration> fields = myconfig.configurationsAt("content");
                for (HierarchicalConfiguration hc : fields) {
                    ParameterItem p = new ParameterItem(hc.getString(".", ""), hc.getString("@type", "static"));
                    parameterList.add(p);
                }
                
                // find the structure elements to be updated
    			DocStruct topstruct = fileformat.getDigitalDocument().getLogicalDocStruct();
    			List<DocStruct> docstructList = new ArrayList<DocStruct>();
    			addAllMatchingDocstructs(docstructList, elements, topstruct);
    			
    			// now run through all matching docstructs to update their values
    			for (DocStruct ds : docstructList) {
    		        
    			    // create metadata value
    	            StringBuilder sb = new StringBuilder();
    	            for (ParameterItem pi : parameterList) {
    	                // replace variables
    	                switch (pi.getType().toLowerCase()) {
    	                
    	                case "variable":
                            // variable from variable replacer
                            pi.setValueToUse(replacer.replace(pi.getValue()));
                            break;
                            
    	                case "metadata":
    	                    // metadata from the same docstruct element
    	                    MetadataType mdt = ughhelp.getMetadataType(prefs, pi.getValue());
    	                    Metadata md = ughhelp.getMetadata(ds, mdt);
    	                    pi.setValueToUse(md.getValue());
    	                    break;
    
    	                case "random":
    	                    // random number with number of digits
    	                    String myId = String.valueOf(ThreadLocalRandom.current().nextInt(1, 999999999 + 1));
    	                    // shorten it, if it is too long
    	                    int length = Integer.valueOf(pi.getValue());
    	                    if (myId.length()> length) {
    	                        myId = myId.substring(0,length);
    	                    }
    	                    // fill it with zeros if it is too short
    	                    myId = StringUtils.leftPad(myId, length, "0");
    	                    pi.setValueToUse(myId);
    	                    break;
    
    	                case "timestamp":
    	                    // timestamp
    	                    long time = System.currentTimeMillis();
    	                    pi.setValueToUse(Long.toString(time));
    	                    break;
    	                    
    	                case "uuid":
    	                    // uuid
    	                    UUID uuid = UUID.randomUUID();
    	                    pi.setValueToUse(uuid.toString());
    	                    break;
    
    	                default:
    	                    pi.setValueToUse(pi.getValue());
    	                    break;
    	                }
    	                
    	                // now add the (changed) value
    	                sb.append(pi.getValueToUse());
    	            }
    	            
    			    // run through all metadata fields to find the correct element
    			    if (forceUpdate) {
    			        // if the value is forced to be written
    	                ughhelp.replaceMetadatum(ds, prefs, field, sb.toString().trim());
    	                updated = true;
    	            } else {
    	                // if the value is not force then only write it if no other metadata of that type exists
    	                MetadataType mdt = ughhelp.getMetadataType(prefs, field);
    	                Metadata md = ughhelp.getMetadata(ds, mdt);
    	                if (md==null || md.getValue() == null || md.getValue().isEmpty()) {
    	                    ughhelp.replaceMetadatum(ds, prefs, field, sb.toString().trim());
    	                    updated = true;
    	                }
    	            }
                }
            }
			
			
			
			if (updated) {
			    process.writeMetadataFile(fileformat);
			}
	        
		} catch (ReadException | PreferencesException | WriteException | IOException | InterruptedException
				| SwapException | DAOException | UghHelperException e) {
		    log.error("Error while automatically updating the metadata", e);
            Helper.setFehlerMeldung("Error while automatically updating metadata.", e);
            Helper.addMessageToProcessLog(step.getProzess().getId(), LogType.ERROR,
                    "Error while automatically updating metadata: " + e.getMessage());
            successfull = false;
		}
        
        log.info("Metadata-update-field step plugin executed");
        if (!successfull) {
            return PluginReturnValue.ERROR;
        }
        return PluginReturnValue.FINISH;
    }
    
    
    /**
     * Run through list of all sub docstructs to put them into a list
     * 
     * @param docstructList List for the docstructs to be added to
     * @param ds current element to check
     */
    private void addAllMatchingDocstructs(List<DocStruct> docstructList, List<String> elements, DocStruct ds) {
        String type = ds.getType().getName();
        if (elements.contains("*") || elements.contains(type)){
            docstructList.add(ds);
        }
        List<DocStruct> children = ds.getAllChildren();
        if (children!=null) {
            for (DocStruct d : children) {
                addAllMatchingDocstructs(docstructList, elements, d);
            }
        }
    }

    @Data
    @RequiredArgsConstructor
    public class ParameterItem {
        @NonNull
        private String value;
        @NonNull
        private String type;
        private String valueToUse;
    }
}
