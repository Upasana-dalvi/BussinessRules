package ext.businessrules;

import com.ptc.core.businessRules.feedback.RuleFeedbackMessage;
import com.ptc.core.businessRules.feedback.RuleFeedbackType;
import com.ptc.core.businessRules.validation.RuleValidation;
import com.ptc.core.businessRules.validation.RuleValidationCriteria;
import com.ptc.core.businessRules.validation.RuleValidationKey;
import com.ptc.core.businessRules.validation.RuleValidationObject;
import com.ptc.core.businessRules.validation.RuleValidationResult;
import com.ptc.core.businessRules.validation.RuleValidationStatus;
 
import wt.fc.ObjectReference;
import wt.fc.Persistable;
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
 
public class Rule3 implements RuleValidation {
 
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
    public RuleValidationResult performValidation(RuleValidationKey paramRuleValidationKey,
                                                  RuleValidationObject paramRuleValidationObject,
                                                  RuleValidationCriteria paramRuleValidationCriteria) throws WTException {
 
        RuleValidationStatus ruleValidationStatus = RuleValidationStatus.SUCCESS;
        RuleValidationResult ruleValidationResult = new RuleValidationResult(ruleValidationStatus);
        Persistable targetObject = paramRuleValidationObject.getTargetObject().getObject();
 
        if (targetObject instanceof WTPart) {
            WTPart wtPart = (WTPart) targetObject;
            ObjectReference objRef = ObjectReference.newObjectReference(wtPart);
 
            // Gather child parts and validate the probability attribute
            String errorMessage = validateChildPartsProbability(wtPart);
            if (errorMessage != null) {
                ruleValidationResult = getValidationResult(objRef, paramRuleValidationKey,
                        new String[]{errorMessage}, "VALIDATE_PROBABILITY_RULE_MESSAGE");
            }
        }
 
        return ruleValidationResult;
    }
 
   private String validateChildPartsProbability(WTPart parentPart) throws WTException {
    QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(parentPart);
    Map<String, List<WTPartUsageLink>> itemGroupLinksMap = new HashMap<>();
 
    // Group child parts by their itemGroup attribute from WTPartUsageLink
    while (qr.hasMoreElements()) {
        WTPartUsageLink link = (WTPartUsageLink) qr.nextElement();
 
        // Load itemGroup from WTPartUsageLink
        PersistableAdapter usageLinkAdapter = new PersistableAdapter(link, null, SessionHelper.getLocale(),
                                                                     new SearchOperationIdentifier());
        usageLinkAdapter.load("com.pluraltechnology.itemgroup");
        String itemGroup = (String) usageLinkAdapter.get("com.pluraltechnology.itemgroup");
 
        if (itemGroup != null && !itemGroup.isEmpty()) {
            itemGroupLinksMap.computeIfAbsent(itemGroup, k -> new ArrayList<>()).add(link);
        }
    }
 
    StringBuilder errorMessages = new StringBuilder();
 
    // Validate each item group
    for (Map.Entry<String, List<WTPartUsageLink>> entry : itemGroupLinksMap.entrySet()) {
        String itemGroup = entry.getKey();
        List<WTPartUsageLink> usageLinks = entry.getValue();
        String validationError = validateProbabilityAttribute(itemGroup, usageLinks);
        if (validationError != null) {
            errorMessages.append(validationError);
        }
    }
 
    System.out.println("Inside the validateChildPartsProbability() function!");
    System.out.println("The error message: " + errorMessages);
 
    return errorMessages.length() > 0 ? errorMessages.toString() : null;
}
 
private String validateProbabilityAttribute(String itemGroup, List<WTPartUsageLink> usageLinks) throws WTException {
    int count100 = 0;
    StringBuilder errorMessages = new StringBuilder();
      System.out.println("inside the validate ProbabilityAttribute() function!");
    for (WTPartUsageLink link : usageLinks) {
        // Load probability attribute from WTPartUsageLink
        PersistableAdapter usageLinkAdapter = new PersistableAdapter(link, null, SessionHelper.getLocale(),
                                                                     new SearchOperationIdentifier());
        usageLinkAdapter.load("com.pluraltechnology.probability");
        Object probablityObj = usageLinkAdapter.get("com.pluraltechnology.probability");
        if(probablityObj != null) {
        	
        
        long probability = (long) usageLinkAdapter.get("com.pluraltechnology.probability");
 
        if (probability == 100l) {
            count100++;
        } else if (probability != 0) {
            errorMessages.append(
                String.format("Invalid probability value for part %s in item group %s. Expected 0 or 100 but got %d.\n",
                        link.getUses().getNumber(), itemGroup, probability));
        }
      }
    }
        
 
    if (count100 != 1) {
        errorMessages.append(
            String.format("Item group %s must have exactly one part with a probability of 100. Found %d.\n",
                    itemGroup, count100));
    }
 
    return errorMessages.length() > 0 ? errorMessages.toString() : null;
}
 
    public RuleValidationResult getValidationResult(WTReference localWTReference, RuleValidationKey paramRuleValidationKey,
                                                    String[] errorMessage, String validationMessage) {
        RuleValidationStatus ruleValidationStatus = RuleValidationStatus.FAILURE;
        RuleValidationResult ruleValidationResult = new RuleValidationResult(ruleValidationStatus);
 
        ruleValidationResult.setTargetObject(localWTReference);
        ruleValidationResult.setValidationKey(paramRuleValidationKey);
 
        RuleFeedbackMessage feedbackMessage = new RuleFeedbackMessage(
                new WTMessage(RESOURCE, validationMessage, errorMessage), RuleFeedbackType.ERROR);
        ruleValidationResult.addFeedbackMessage(feedbackMessage);
 
        return ruleValidationResult;
    }
 
    @Override
    public void prepareForValidation(RuleValidationKey arg0, RuleValidationCriteria arg1) throws WTException {
        // Optional pre-validation setup
    }
}