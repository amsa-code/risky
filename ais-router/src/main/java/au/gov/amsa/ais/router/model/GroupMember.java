package au.gov.amsa.ais.router.model;

import rx.Observable;

public interface GroupMember {

    Observable<String> lines();
}
