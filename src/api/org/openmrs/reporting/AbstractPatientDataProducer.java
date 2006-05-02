package org.openmrs.reporting;

import java.util.Date;
import org.openmrs.User;

public abstract class AbstractPatientDataProducer extends AbstractReportObject {

	public AbstractPatientDataProducer()
	{
		// do nothing
		super.setType("Patient Data Producer");
	}

	public AbstractPatientDataProducer(Integer reportObjectId, String name, String description, String type, String subType, 
			User creator, Date dateCreated, User changedBy, Date dateChanged, Boolean voided, User voidedBy,
			Date dateVoided, String voidReason )
	{
		super(reportObjectId, name, description, type, subType, creator, dateCreated, changedBy, dateChanged, voided, voidedBy,
				dateVoided, voidReason);
	}
}
