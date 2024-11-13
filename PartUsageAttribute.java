package ext.businessrules;

import com.ptc.core.businessRules.feedback.RuleFeedbackMessage;
import com.ptc.core.businessRules.feedback.RuleFeedbackType;
import com.ptc.core.businessRules.validation.RuleValidation;
import com.ptc.core.businessRules.validation.RuleValidationCriteria;
import com.ptc.core.businessRules.validation.RuleValidationKey;
import com.ptc.core.businessRules.validation.RuleValidationObject;
import com.ptc.core.businessRules.validation.RuleValidationResult;
import com.ptc.core.businessRules.validation.RuleValidationStatus;
 
import wt.change2.WTChangeOrder2;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTReference;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartUsageLink;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.util.WTMessage;
 
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.SearchOperationIdentifier;
 
import java.util.HashMap;
import java.util.List;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
 
public class PartUsageAttribute implements RuleValidation {
 
    private static final String RESOURCE = "ext.RB.IGBusinessRuleRB";
 
    @Override
    public boolean isConfigurationValid(RuleValidationKey arg0) {
        return true;
    }
 
    @Override
    public Class[] getSupportedClasses(RuleValidationKey arg0) {
        return new Class[]{Persistable.class};
    }
 
    @Override
    public RuleValidationResult performValidation(RuleValidationKey paramRuleValidationKey, RuleValidationObject paramRuleValidationObject,
                                                  RuleValidationCriteria paramRuleValidationCriteria) throws WTException {
 
        RuleValidationStatus rulevalidationstatus = RuleValidationStatus.SUCCESS;
        RuleValidationResult rulevalidationresult = new RuleValidationResult(rulevalidationstatus);
        Persistable targetObject = paramRuleValidationObject.getTargetObject().getObject();
 
        if (targetObject instanceof WTPart) {
            WTPart wtpart = (WTPart) targetObject;
            ObjectReference objRef = ObjectReference.newObjectReference(wtpart);
 
            // Check ITEM_GROUP validation
            String errorMessage = validateItemGroup(wtpart);
            if (errorMessage != null) {
                rulevalidationresult = getValidationResult(objRef, paramRuleValidationKey, new String[]{errorMessage}, "ITEM_GROUP_VALIDATION_ERROR");
            }
        }
 
        return rulevalidationresult;
    }
 
    @SuppressWarnings("null")
	private String validateItemGroup(WTPart parentPart) throws WTException {
        QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(parentPart);
        Map<String, Integer> itemGroupMap = new HashMap<>();
        Map<String, List<WTPartMaster>> itemGroupPartsMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();

        while (qr.hasMoreElements()) {
            WTPartUsageLink link = (WTPartUsageLink) qr.nextElement();
            WTPartMaster subWTP = (WTPartMaster) link.getUses();
 
            // Fetch ITEM_GROUP IBA value
            PersistableAdapter obj = new PersistableAdapter(link, null, SessionHelper.getLocale(), new SearchOperationIdentifier());
            obj.load("com.pluraltechnology.itemgroup");
            String itemGroup = (String) obj.get("com.pluraltechnology.itemgroup");
 
            if (itemGroup != null) {
                itemGroupMap.put(itemGroup, itemGroupMap.getOrDefault(itemGroup, 0) + 1);
                // Add part to the list of parts under this ITEM_GROUP
                itemGroupPartsMap.computeIfAbsent(itemGroup, k -> new ArrayList<>()).add(subWTP);
            }
 
            System.out.println("Name: " + subWTP.getName() + " Number: " + subWTP.getNumber() + " ITEM_GROUP: " + itemGroup);
        }
 
        System.out.println("Value"+itemGroupMap.get("a2"));
        // Traverse the map to find any ITEM_GROUP with frequency less than 2
        for (Map.Entry<String, Integer> entry : itemGroupMap.entrySet()) {
            if (entry.getValue() < 2 || entry.getValue() > getMaxValue()) {
                List<WTPartMaster> problematicParts = itemGroupPartsMap.get(entry.getKey());
                sb.append("ITEM_GROUP").append(entry.getKey()).append("' has less than 2 or more than 5 occurrences. Problematic Parts: ");
                for (WTPartMaster part : problematicParts) {
                	 sb.append("Name: ").append(part.getName())
                     .append(", Number: ").append(part.getNumber()).append("; /n");
                }
                

            }
        }
 
        // Return null if no issue is found
        return sb.toString();
    }
    
    public int getMaxValue() {
		int maxValue = 0;
		Properties properties = new Properties();
 
		try {
			FileInputStream inputStream = new FileInputStream("D:\\ptc\\Windchill_12.1\\Windchill\\src\\ext\\businessrules\\itemGroupMaxVal.properties");
			properties.load(inputStream);
			inputStream.close();
 
			maxValue = Integer.parseInt(properties.getProperty("maxValue"));
			System.out.println("maxValue: "+maxValue);
		}catch(Exception e) {
			System.out.println(e.toString());
		}
 
		return maxValue;
	}
 
    public RuleValidationResult getValidationResult(WTReference localWTReference, RuleValidationKey paramRuleValidationKey, String[] errorMessage, String validationMessage) {
        RuleValidationStatus ruleValidationStatus = RuleValidationStatus.FAILURE;
        RuleValidationResult ruleValidationResult = new RuleValidationResult(ruleValidationStatus);
 
        ruleValidationResult.setTargetObject(localWTReference);
        ruleValidationResult.setValidationKey(paramRuleValidationKey);
 
        RuleFeedbackMessage feedbackMessage = new RuleFeedbackMessage(new WTMessage(RESOURCE, validationMessage, errorMessage), RuleFeedbackType.ERROR);
        ruleValidationResult.addFeedbackMessage(feedbackMessage);
        return ruleValidationResult;
    }
 
    @Override
    public void prepareForValidation(RuleValidationKey arg0, RuleValidationCriteria arg1) throws WTException {
        // Optional pre-validation setup
    }
}