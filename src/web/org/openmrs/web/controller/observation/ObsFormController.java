package org.openmrs.web.controller.observation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.web.WebConstants;
import org.openmrs.web.propertyeditor.LocationEditor;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

public class ObsFormController extends SimpleFormController {
	
    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());
    
    SimpleDateFormat dateFormat;
    
	/**
	 * 
	 * Allows for Integers to be used as values in input tags.
	 *   Normally, only strings and lists are expected 
	 * 
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest, org.springframework.web.bind.ServletRequestDataBinder)
	 */
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
		super.initBinder(request, binder);
		Context context = (Context) request.getSession().getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		
		dateFormat = new SimpleDateFormat(OpenmrsConstants.OPENMRS_LOCALE_DATE_PATTERNS().get(context.getLocale().toString().toLowerCase()), context.getLocale());
		
        binder.registerCustomEditor(java.lang.Integer.class,
                new CustomNumberEditor(java.lang.Integer.class, true));
        binder.registerCustomEditor(java.util.Date.class, 
        		new CustomDateEditor(dateFormat, true));
        binder.registerCustomEditor(Location.class, new LocationEditor(context));
        binder.registerCustomEditor(java.lang.Boolean.class,
        		new CustomBooleanEditor(true)); //allow for an empty boolean value
	}

	protected ModelAndView processFormSubmission(HttpServletRequest request, HttpServletResponse reponse, Object obj, BindException errors) throws Exception {
		
		HttpSession httpSession = request.getSession();
		Context context = (Context) httpSession.getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		
		// sets the objects in case edit Reason is rejected
		Obs obs = (Obs)obj;
		obs = setObjects(obs, request);
    	
    	String reason = request.getParameter("editReason");
    	if (obs.getObsId() != null && (reason == null || reason.length() == 0))
    		errors.reject("editReason", "Obs.edit.reason.empty");

		return super.processFormSubmission(request, reponse, obs, errors);
	}

	/** 
	 * 
	 * The onSubmit function receives the form/command object that was modified
	 *   by the input form and saves it to the db
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object obj, BindException errors) throws Exception {
		
		HttpSession httpSession = request.getSession();
		Context context = (Context) httpSession.getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		String view = getFormView();
		
		if (context != null && context.isAuthenticated()) {
			Obs obs = (Obs)obj;
			obs = setObjects(obs, request);
			ObsService os = context.getObsService();
			if (obs.getObsId() == null)
				os.createObs(obs);
			else {
				if (obs.isVoided()) {
					// either the user just clicked the void checkbox, or is updating a voided obs (for whatever reason)
					String reason = obs.getVoidReason();
					if (reason != null && reason.length() > 0)
						reason += ", ";
					os.voidObs(obs, reason + request.getParameter("editReason"));
				}
				else {
					//save the current obsId so we can void this obs after creating the new one
					Integer oldObsId = obs.getObsId();
	
					context.endTransaction();
					context.startTransaction();
					
					//and recreate the obs as this editor
					obs.setObsId(null);
					obs.setCreator(context.getAuthenticatedUser());
					obs.setDateCreated(new Date());
					os.updateObs(obs);
					Integer newObsId = obs.getObsId();
					
					context.endTransaction();
					context.startTransaction();
					
					Obs oldObs = os.getObs(oldObsId);
					os.voidObs(oldObs, request.getParameter("editReason") + " (new obsId: " + newObsId + ")");
				}
			}
			view = getSuccessView();
			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Obs.saved");

			view = view + "?encounterId=" + obs.getEncounter().getEncounterId() + "&phrase=" + request.getParameter("phrase");
		}
		
		return new ModelAndView(new RedirectView(view));
	}

	/**
	 * 
	 * This is called prior to displaying a form for the first time.  It tells Spring
	 *   the form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
    protected Object formBackingObject(HttpServletRequest request) throws ServletException {

		HttpSession httpSession = request.getSession();
		Context context = (Context) httpSession.getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		
		Obs obs = null;
		
		if (context != null && context.isAuthenticated()) {
			ObsService os = context.getObsService();
			EncounterService es = context.getEncounterService();
			
			String obsId = request.getParameter("obsId");
	    	String encounterId = request.getParameter("encounterId");
	    	
			if (obsId != null)
	    		obs = os.getObs(Integer.valueOf(obsId));
	    	else if (encounterId != null) {
	    		Encounter e = es.getEncounter(Integer.valueOf(encounterId));
	    		obs = new Obs();
	    		obs.setEncounter(e);
	    		obs.setPatient(e.getPatient());
	    		obs.setLocation(e.getLocation());
	    		obs.setObsDatetime(e.getEncounterDatetime());
	    	}
		}
		
		if (obs == null)
			obs = new Obs();
    	
        return obs;
    }

	protected Map referenceData(HttpServletRequest request, Object obj, Errors errs) throws Exception {
		
		HttpSession httpSession = request.getSession();
		Context context = (Context) httpSession.getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);

		Obs obs = (Obs)obj;
		
		Map<String, Object> map = new HashMap<String, Object>();
		String defaultVerbose = "false";
		
		if (context != null && context.isAuthenticated()) {
			ObsService es = context.getObsService();
			//map.put("obsTypes", es.getObsTypes());
			map.put("forms", context.getFormService().getForms());
			if (obs.getConcept() != null)
				map.put("conceptName", obs.getConcept().getName(request.getLocale()));
			defaultVerbose = context.getAuthenticatedUser().getProperty(OpenmrsConstants.USER_PROPERTY_SHOW_VERBOSE);
		}
		map.put("datePattern", dateFormat.toLocalizedPattern().toLowerCase());

		map.put("defaultVerbose", defaultVerbose.equals("true") ? true : false);
		
		String editReason = request.getParameter("editReason");
		if (editReason == null)
			editReason = "";
		
		map.put("editReason", editReason);
		
		return map;
	}
	
	private Obs setObjects(Obs obs, HttpServletRequest request) {

		HttpSession httpSession = request.getSession();
		Context context = (Context) httpSession.getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);

		if (context != null && context.isAuthenticated()) {
			if (obs.getObsId() == null) { //patient/order/concept/encounter only change when adding a new observation
				if (StringUtils.hasText(request.getParameter("patientId")))
					obs.setPatient(context.getPatientService().getPatient(Integer.valueOf(request.getParameter("patientId"))));
				else
					obs.setPatient(null);
				if (StringUtils.hasText(request.getParameter("orderId")))
					obs.setOrder(context.getOrderService().getOrder(Integer.valueOf(request.getParameter("orderId"))));
				else
					obs.setOrder(null);
				if (StringUtils.hasText(request.getParameter("conceptId")))
					obs.setConcept(context.getConceptService().getConcept(Integer.valueOf(request.getParameter("conceptId"))));
				else
					obs.setConcept(null);
				if (StringUtils.hasText(request.getParameter("encounterId")))
					obs.setEncounter(context.getEncounterService().getEncounter(Integer.valueOf(request.getParameter("encounterId"))));
				else
					obs.setEncounter(null);
				
			}
			
			if (StringUtils.hasText(request.getParameter("valueCodedId")))
				obs.setValueCoded(context.getConceptService().getConcept(Integer.valueOf(request.getParameter("valueCodedId"))));
			else
				obs.setValueCoded(null);
			if (StringUtils.hasText(request.getParameter("valueDrugId")))
				obs.setValueDrug(context.getConceptService().getDrug(Integer.valueOf(request.getParameter("valueDrugId"))));
			else
				obs.setValueDrug(null);
		}
		
		return obs;

	}

}