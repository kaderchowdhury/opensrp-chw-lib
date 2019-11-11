package org.smartregister.chw.interactor;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import org.smartregister.chw.core.dao.VisitDao;
import org.smartregister.chw.core.domain.VisitSummary;
import org.smartregister.chw.core.enums.ImmunizationState;
import org.smartregister.chw.core.interactor.CoreFamilyInteractor;
import org.smartregister.chw.core.model.ChildVisit;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.util.ChildUtils;
import org.smartregister.chw.util.Constants;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.family.util.DBConstants;

import java.util.Map;

public class FamilyInteractor extends CoreFamilyInteractor {

    @Override
    public ImmunizationState getChildStatus(Context context, final String childId, Cursor cursor) {
        CommonPersonObject personObject = org.smartregister.family.util.Utils.context().commonrepository(Constants.TABLE_NAME.CHILD).findByBaseEntityId(cursor.getString(1));
        if (!personObject.getCaseId().equalsIgnoreCase(childId)) {

            String dobString = org.smartregister.util.Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.DOB, false);

            Map<String, VisitSummary> map = VisitDao.getVisitSummary(childId);
            long lastHomeVisit = 0;
            long visitNotDone = 0;

            if (map != null) {
                VisitSummary notDone = map.get(CoreConstants.EventType.CHILD_VISIT_NOT_DONE);
                VisitSummary lastVisit = map.get(CoreConstants.EventType.CHILD_HOME_VISIT);

                if (lastVisit != null) {
                    lastHomeVisit = lastVisit.getVisitDate().getTime();
                }

                if (notDone != null) {
                    visitNotDone = notDone.getVisitDate().getTime();
                }
            }

            String strDateCreated = org.smartregister.family.util.Utils.getValue(personObject.getColumnmaps(), ChildDBConstants.KEY.DATE_CREATED, false);

            long dateCreated = 0;
            if (!TextUtils.isEmpty(strDateCreated)) {
                dateCreated = org.smartregister.family.util.Utils.dobStringToDateTime(strDateCreated).getMillis();
            }

            final ChildVisit childVisit = ChildUtils.getChildVisitStatus(context, dobString, lastHomeVisit, visitNotDone, dateCreated);
            return getImmunizationStatus(childVisit.getVisitStatus());
        }
        return ImmunizationState.NO_ALERT;
    }
}
