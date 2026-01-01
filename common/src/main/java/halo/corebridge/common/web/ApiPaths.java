package halo.corebridge.common.web;

public final class ApiPaths {

    private ApiPaths() {} // 인스턴스 생성 방지

    public static final String API = "/api";
    public static final String V1 = API + "/v1";

    public static final String JOB_POSTINGS = V1 + "/jobpostings";
    public static final String USERS = V1 + "/users";
}
