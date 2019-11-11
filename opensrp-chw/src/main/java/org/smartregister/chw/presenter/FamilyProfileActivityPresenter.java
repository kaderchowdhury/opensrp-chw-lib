package org.smartregister.chw.presenter;

import org.smartregister.chw.anc.repository.VisitRepository;
import org.smartregister.chw.core.utils.ChildDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.model.WashCheckModel;
import org.smartregister.family.contract.FamilyProfileActivityContract;
import org.smartregister.family.presenter.BaseFamilyProfileActivityPresenter;
import org.smartregister.family.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class FamilyProfileActivityPresenter extends BaseFamilyProfileActivityPresenter {
    private WashCheckModel washCheckModel;

    public FamilyProfileActivityPresenter(FamilyProfileActivityContract.View view, FamilyProfileActivityContract.Model model, String viewConfigurationIdentifier, String familyBaseEntityId) {
        super(view, model, viewConfigurationIdentifier, familyBaseEntityId);
        washCheckModel = new WashCheckModel(familyBaseEntityId);
    }

    @Override
    public void initializeQueries(String mainCondition) {
        String tableName = VisitRepository.VISIT_TABLE;

        String countSelect = model.countSelect(tableName, mainCondition);
        String mainSelect = model.mainSelect(tableName, mainCondition);

        getView().initializeQueryParams(Utils.metadata().familyActivityRegister.tableName, countSelect, mainSelect);
        getView().initializeAdapter(visibleColumns);

        getView().countExecute();
        getView().filterandSortInInitializeQueries();
    }

    @Override
    public String getMainCondition() {
        return "(ec_family_member.relational_id = '" + familyBaseEntityId + "' or visits.base_entity_id = '" + familyBaseEntityId + "') AND " +
                ChildDBConstants.KEY.VISIT_TYPE + " in ( " + getEventsInCSV() + ") ";
    }

    private String getEventsInCSV() {
        List<String> events = new ArrayList<>();
        events.add(CoreConstants.EventType.ANC_HOME_VISIT);
        events.add(CoreConstants.EventType.PNC_HOME_VISIT);
        events.add(CoreConstants.EventType.ANC_HOME_VISIT_NOT_DONE);
        events.add(CoreConstants.EventType.CHILD_HOME_VISIT);
        events.add(CoreConstants.EventType.CHILD_VISIT_NOT_DONE);
        events.add(CoreConstants.EventType.WASH_CHECK);

        StringBuilder res = new StringBuilder();
        for (String s : events) {
            if (res.length() > 0)
                res.append(",");

            res.append("'").append(s).append("'");
        }

        return res.toString();
    }

    @Override
    public String getDefaultSortQuery() {
        return VisitRepository.VISIT_TABLE + "." + ChildDBConstants.KEY.VISIT_DATE + " DESC";
    }
}
