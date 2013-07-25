/**
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/ 
* 
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
* 
* The Original Code is OpenELIS code.
* 
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*/
package us.mn.state.health.lims.sample.action;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.DynaActionForm;

import us.mn.state.health.lims.common.action.BaseAction;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.util.validator.ActionError;
import us.mn.state.health.lims.common.log.LogEvent;
import us.mn.state.health.lims.sample.dao.SampleDAO;
import us.mn.state.health.lims.sample.daoimpl.SampleDAOImpl;
import us.mn.state.health.lims.sample.valueholder.Sample;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.login.valueholder.UserSessionData;

/**
 * @author diane benz
 * 
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of type
 * comments go to Window>Preferences>Java>Code Generation.
 */
public class SampleDeleteAction extends BaseAction {
	static private String FWD_CLOSE = "close";

	protected ActionForward performAction(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// The first job is to determine if we are coming to this action with an
		// ID parameter in the request. If there is no parameter, we are
		// creating a new Sample.
		// If there is a parameter present, we should bring up an existing
		// Sample to edit.
		String forward = "success";
		//System.out.println("I am in SampleDeleteAction");

		DynaActionForm dynaForm = (DynaActionForm) form;
		// get selected samples
		String[] selectedIDs = (String[]) dynaForm.get("selectedIDs");

		// Vector samples = new Vector();
		//get sysUserId from login module
		UserSessionData usd = (UserSessionData)request.getSession().getAttribute(USER_SESSION_DATA);
		String sysUserId = String.valueOf(usd.getSystemUserId());	

		List samples = new ArrayList();

		for (int i = 0; i < selectedIDs.length; i++) {
			Sample sample = new Sample();
			sample.setId(selectedIDs[i]);
			sample.setSysUserId(sysUserId);
			samples.add(sample);
		}

		org.hibernate.Transaction tx = HibernateUtil.getSession().beginTransaction();
		ActionMessages errors = null;		
		try {
			// selectedIDs = (List)PropertyUtils.getProperty(dynaForm,
			// "selectedIDs");
			//System.out.println("Going to delete Sample");
			SampleDAO sampleDAO = new SampleDAOImpl();
			sampleDAO.deleteData(samples);
			//System.out.println("Just deleted Sample");
			// initialize the form
			dynaForm.initialize(mapping);
			// repopulate the form
			// PropertyUtils.copyProperties(dynaForm, sample);
			// PropertyUtils.setProperty(dynaForm, "samples", samples);
	
			// PropertyUtils.setProperty(dynaForm, "selectedIDs", selectedIDs);
			tx.commit();
		} catch (LIMSRuntimeException lre) {
    		//bugzilla 2154
			LogEvent.logError("SampleDeleteAction","performAction()",lre.toString());
			tx.rollback();
			
			errors = new ActionMessages();
			ActionError error = null;
			if (lre.getException() instanceof org.hibernate.StaleObjectStateException) {
				error = new ActionError("errors.OptimisticLockException", null,	null);
			} else {
				error = new ActionError("errors.DeleteException", null, null);
			}
			errors.add(ActionMessages.GLOBAL_MESSAGE, error);
			saveErrors(request, errors);
			request.setAttribute(Globals.ERROR_KEY, errors);
			forward = FWD_FAIL;
						
		}  finally {
            HibernateUtil.closeSession();
        }	
		if (forward.equals(FWD_FAIL))
			return mapping.findForward(forward);
		
		if ("true".equalsIgnoreCase(request.getParameter("close"))) {
			forward = FWD_CLOSE;
		}

		request.setAttribute("menuDefinition", "SampleMenuDefinition");

		return mapping.findForward(forward);
	}

	protected String getPageTitleKey() {
		return null;
	}

	protected String getPageSubtitleKey() {
		return null;
	}
}