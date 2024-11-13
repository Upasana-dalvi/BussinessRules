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
 
public class Rule4 implements RuleValidation {
 
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
    public RuleValidationResult performValidation(RuleValidationKey paramRuleValidationKey, RuleValidationObject paramRuleValidationObject,RuleValidationCriteria paramRuleValidationCriteria) throws WTException {
 
        RuleValidationStatus rulevalidationstatus = RuleValidationStatus.SUCCESS;
        WTChangeOrder2 cn = (WTChangeOrder2) paramRuleValidationCriteria.getPrimaryBusinessObject();
        RuleValidationResult rulevalidationresult = new RuleValidationResult(rulevalidationstatus);
        Persistable targetObject = paramRuleValidationObject.getTargetObject().getObject();
 
        if (targetObject instanceof WTPart) {
            WTPart wtpart = (WTPart) targetObject;
            ObjectReference objRef = ObjectReference.newObjectReference(wtpart);
 
            // Check if probability, strategy, priority are filled but itemgroup is not
            String validationMessage = validateRequiredFields(wtpart);
            if (validationMessage != null) {
                String[] errorMessage = {validationMessage, cn.getNumber()};
                rulevalidationresult = getValidationResult(objRef, paramRuleValidationKey, errorMessage, "VALIDATE_REQUIRED_FIELDS_ERROR_MESSAGE");
            }
        }
 
        return rulevalidationresult;
    }
 
    private String validateRequiredFields(WTPart parentPart) throws WTException {
        QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(parentPart);
        StringBuilder validationMessages = new StringBuilder();
        while (qr.hasMoreElements()) {
            WTPartUsageLink link = (WTPartUsageLink) qr.nextElement();
            WTPartMaster subWTP = (WTPartMaster) link.getUses();
            // Fetch ITEM_GROUP, probability, strategy, and priority IBA values
            PersistableAdapter obj = new PersistableAdapter(link, null, SessionHelper.getLocale(), new SearchOperationIdentifier());
            obj.load("com.pluraltechnology.itemgroup", "com.pluraltechnology.probability", "com.pluraltechnology.priority",	
            		"com.pluraltechnology.Strategy");
 
            String itemGroup = (String) obj.get("com.pluraltechnology.itemgroup");
            Object probabilityObj = obj.get("com.pluraltechnology.probability");
            Object priorityObj = obj.get("com.pluraltechnology.priority");
            Object strategyObj = obj.get("com.pluraltechnology.Strategy");
 
            // Convert probability, strategy, and priority to String if they are not null
            String probability = (probabilityObj != null) ? probabilityObj.toString() : "";
            String priority = (priorityObj != null) ? priorityObj.toString() : "";
            String strategy = (strategyObj != null) ? strategyObj.toString() : "";
 
            // Validation: if probability, strategy, and priority are filled, check that itemgroup is also present
            if ((probability != null && !probability.isEmpty()) &&
                (priority != null && !priority.isEmpty()) && (strategy != null && !strategy.isEmpty())) {
 
                if (itemGroup == null || itemGroup.isEmpty()) {
                    validationMessages.append("Part with number ")
                        .append(subWTP.getNumber())
                        .append(" has 'probability', 'strategy', and 'priority' filled but 'ITEM_GROUP' is missing.\n");
                }
            }
        }
 
        
        return validationMessages.length() > 0 ? validationMessages.toString() : null;
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
      
    }
}

