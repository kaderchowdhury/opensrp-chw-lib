package org.smartregister.chw.interactor;

import android.content.Context;
import android.util.Pair;

import org.jeasy.rules.api.Rules;
import org.joda.time.LocalDate;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.contract.BaseAncMemberProfileContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.model.BaseUpcomingService;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.contract.PncMemberProfileContract;
import org.smartregister.chw.core.contract.CoreChildProfileContract;
import org.smartregister.chw.core.interactor.CorePncMemberProfileInteractor;
import org.smartregister.chw.core.rule.PncVisitAlertRule;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.core.utils.HomeVisitUtil;
import org.smartregister.chw.pnc.PncLibrary;
import org.smartregister.chw.util.ScheduleUtil;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.dao.AbstractDao;
import org.smartregister.domain.Alert;
import org.smartregister.domain.AlertStatus;
import org.smartregister.repository.AllSharedPreferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class PncMemberProfileInteractor extends CorePncMemberProfileInteractor implements PncMemberProfileContract.Interactor {
    private Context context;
    private Date lastVisitDate;
    private Date deliveryDate;

    public PncMemberProfileInteractor(Context context) {
        this.context = context;
    }

    /**
     * Compute and process the lower profile info
     *
     * @param memberObject
     * @param callback
     */
    @Override
    public void refreshProfileInfo(final MemberObject memberObject, final BaseAncMemberProfileContract.InteractorCallBack callback) {
        Runnable runnable = new Runnable() {
            Date lastVisitDate = getLastVisitDate(memberObject);
            AlertStatus familyAlert = ScheduleUtil.getFamilyAlertStatus(context, memberObject.getBaseEntityId(), memberObject.getFamilyBaseEntityId());
            Alert upcomingService = getAlerts(context, memberObject);

            @Override
            public void run() {
                appExecutors.mainThread().execute(() -> {
                    callback.refreshLastVisit(lastVisitDate);
                    callback.refreshFamilyStatus(familyAlert);

                    if (upcomingService == null) {
                        callback.refreshUpComingServicesStatus("", AlertStatus.complete, new Date());
                    } else {
                        callback.refreshUpComingServicesStatus(upcomingService.scheduleName(), upcomingService.status(), new LocalDate(upcomingService.startDate()).toDate());
                    }
                });
            }
        };
        appExecutors.diskIO().execute(runnable);
    }

    private Date getLastVisitDate(MemberObject memberObject) {
        Date lastVisitDate = null;
        Visit lastVisit = AncLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.PNC_HOME_VISIT);
        if (lastVisit != null) {
            lastVisitDate = lastVisit.getDate();
        }

        return lastVisitDate;
    }


    private Alert getAlerts(Context context, MemberObject memberObject) {
        PncUpcomingServicesInteractorFlv upcomingServicesInteractor = new PncUpcomingServicesInteractorFlv();
        try {
            List<BaseUpcomingService> baseUpcomingServices = upcomingServicesInteractor.getMemberServices(context, memberObject);
            if (baseUpcomingServices.size() > 0) {
                Comparator<BaseUpcomingService> comparator = (o1, o2) -> o1.getServiceDate().compareTo(o2.getServiceDate());
                Collections.sort(baseUpcomingServices, comparator);

                BaseUpcomingService baseUpcomingService = baseUpcomingServices.get(0);
                String dateToDisplay = ((baseUpcomingService.getOverDueDate().before(new LocalDate().toDate())) || (baseUpcomingService.getOverDueDate().equals( new LocalDate().toDate()))) ? AbstractDao.getDobDateFormat().format(baseUpcomingService.getOverDueDate()) : AbstractDao.getDobDateFormat().format(baseUpcomingService.getServiceDate());

                return new Alert(
                        memberObject.getBaseEntityId(),
                        baseUpcomingService.getServiceName(),
                        baseUpcomingService.getServiceName(),
                        baseUpcomingService.getOverDueDate().before(new LocalDate().toDate()) || baseUpcomingService.getOverDueDate().equals( new LocalDate().toDate()) ? AlertStatus.urgent : AlertStatus.normal,
                        dateToDisplay,
                        "",
                        true
                );
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return null;
    }

    public void updateChild(final Pair<Client, Event> pair, final String jsonString, final CoreChildProfileContract.InteractorCallBack callBack) {
        Runnable runnable = () -> appExecutors.mainThread().execute(() -> {
            try {
                new ChildProfileInteractor().saveRegistration(pair, jsonString, true, callBack);
            } catch (Exception e) {
                Timber.e(e);
            }
        });
        appExecutors.diskIO().execute(runnable);
    }

    public PncVisitAlertRule getVisitSummary(String motherBaseID) {
        Rules rules = ChwApplication.getInstance().getRulesEngineHelper().rules(org.smartregister.chw.util.Constants.RULE_FILE.PNC_HOME_VISIT);
        Date lastVisitDate = getLastDateVisit(motherBaseID);
        Date deliveryDate = getDeliveryDate(motherBaseID);
        return HomeVisitUtil.getPncVisitStatus(rules, lastVisitDate, deliveryDate);
    }

    private Date getLastDateVisit(String motherBaseID) {
        Visit lastVisit = AncLibrary.getInstance().visitRepository().getLatestVisit(motherBaseID, org.smartregister.chw.anc.util.Constants.EVENT_TYPE.PNC_HOME_VISIT);
        if (lastVisit != null) {
            lastVisitDate = lastVisit.getDate();
            return lastVisitDate;
        } else {
            return lastVisitDate = getDeliveryDate(motherBaseID);
        }

    }

    private Date getDeliveryDate(String motherBaseID) {
        String deliveryDateString = PncLibrary.getInstance().profileRepository().getDeliveryDate(motherBaseID);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        try {
            deliveryDate = sdf.parse(deliveryDateString);
        } catch (ParseException e) {
            Timber.e(e);
        }
        return deliveryDate;
    }

    @Override
    public void createReferralEvent(AllSharedPreferences allSharedPreferences, String jsonString, String entityID) throws Exception {
        CoreReferralUtils.createReferralEvent(allSharedPreferences, jsonString, CoreConstants.TABLE_NAME.PNC_REFERRAL, entityID);
    }
}
