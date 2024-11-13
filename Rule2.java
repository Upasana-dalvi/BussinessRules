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
import java.util.ArrayList;
import java.util.Map;
 
public class Rule2 implements RuleValidation {
 
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
            String errorMessage = validatePriorityAndProbability(wtpart);
            if (errorMessage != null) {
                rulevalidationresult = getValidationResult(objRef, paramRuleValidationKey, new String[]{errorMessage}, "VALIDATE_PRIORITY_AND_PROBABLITY_RULE_ERROR");
            }
        }
 
        return rulevalidationresult;
    }
 
    private String validatePriorityAndProbability(WTPart part) throws WTException {
        // Fetch the usage links of the part
        QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(part);

        while (qr.hasMoreElements()) {
            WTPartUsageLink usageLink = (WTPartUsageLink) qr.nextElement();
            WTPartMaster subWTP = (WTPartMaster) usageLink.getUses();

            // Get IBAs from the usage link
            PersistableAdapter linkAdapter = new PersistableAdapter(usageLink, null, SessionHelper.getLocale(), new SearchOperationIdentifier());
            linkAdapter.load("com.pluraltechnology.itemgroup", "com.pluraltechnology.probability", "com.pluraltechnology.priority");

            String itemGroup = (String) linkAdapter.get("com.pluraltechnology.itemgroup");

            // Get probability and priority as Object
            Object probabilityObj = linkAdapter.get("com.pluraltechnology.probability");
            Object priorityObj = linkAdapter.get("com.pluraltechnology.priority");

        

            // Convert Long values to String, ensuring nulls are handled gracefully
            Long probability = (Long)probabilityObj;
            String priority = priorityObj != null ? String.valueOf(priorityObj) : "";

            // Validation logic: If ITEM_GROUP is filled, both probability and priority must also be filled
            if (itemGroup != null && !itemGroup.isEmpty()) {
                if (probability!=null || priority.isEmpty()) {
                    String partNumber = subWTP.getNumber();  // Assuming you have access to the part's number
                    return "For part '" + partNumber + "', ITEM_GROUP '" + itemGroup + "' is filled, but both Probability and Priority must also be filled for the usage link.";
                }
            }
        }

        // Return null if no validation errors
        return null;
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