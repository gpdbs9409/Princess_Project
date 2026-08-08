package com.example.princessproject.admin.controller;

import com.example.princessproject.admin.dto.AdjustmentRequest;
import com.example.princessproject.admin.dto.AdjustmentResponse;
import com.example.princessproject.admin.dto.AdminApplicantResponse;
import com.example.princessproject.admin.dto.AdminMemberResponse;
import com.example.princessproject.admin.dto.AdminMemberWeekResponse;
import com.example.princessproject.admin.dto.CohortRequest;
import com.example.princessproject.admin.dto.MvpRequest;
import com.example.princessproject.admin.dto.MvpResponse;
import com.example.princessproject.admin.dto.RecruitmentApplicantRequest;
import com.example.princessproject.admin.dto.RecruitmentApplicantResponse;
import com.example.princessproject.admin.dto.RefundRequest;
import com.example.princessproject.admin.service.AdminService;
import com.example.princessproject.user.dto.UserResponse;
import com.example.princessproject.user.model.Role;
import com.example.princessproject.user.service.UserService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Everything under here is gated to ROLE_ADMIN in SecurityConfig - same DB, same server as
 * the rest of the app. The users.role column decides who is staff; the first admin has to be
 * promoted with a direct UPDATE, after which /users/{id}/role handles the rest.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    /** 운영진 승격/강등. users.role이 기준이라 로그인해도 덮어써지지 않는다. */
    @PutMapping("/users/{userId}/role")
    public UserResponse setRole(@PathVariable Long userId, @RequestParam Role role) {
        return UserResponse.from(userService.setRole(userId, role));
    }

    @GetMapping("/cohorts")
    public List<String> cohorts() {
        return adminService.listCohorts();
    }

    /** 지원자 리스트 - 아직 기수가 배정되지 않은 회원. */
    @GetMapping("/applicants")
    public List<AdminApplicantResponse> applicants() {
        return adminService.listApplicants();
    }

    /** 참가자 리스트 - 기수가 배정된 회원의 주간 활동/환급 현황. cohort 생략 시 전체 기수. */
    @GetMapping("/participants")
    public List<AdminMemberWeekResponse> participants(
            @RequestParam(required = false) String cohort,
            @RequestParam(required = false) LocalDate weekStart
    ) {
        return adminService.listParticipantsForWeek(cohort, resolveWeekStart(weekStart));
    }

    @PutMapping("/members/{userId}/cohort")
    public AdminMemberResponse assignCohort(@PathVariable Long userId, @RequestBody CohortRequest request) {
        return AdminMemberResponse.from(adminService.assignCohort(userId, request.cohort()));
    }

    @PutMapping("/members/{userId}/refund")
    public AdminMemberWeekResponse setRefund(
            @PathVariable Long userId,
            @RequestParam(required = false) LocalDate weekStart,
            @RequestBody RefundRequest request
    ) {
        return adminService.setRefundPaid(userId, resolveWeekStart(weekStart), request.paid());
    }

    /** 이번 주 MVP 지정 - 같은 기수/주에 이미 있던 MVP는 교체된다. */
    @PutMapping("/mvp")
    public MvpResponse setMvp(@RequestBody MvpRequest request) {
        return adminService.setMvp(request.userId(), resolveWeekStart(request.weekStart()), request.note());
    }

    @DeleteMapping("/mvp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearMvp(@RequestParam String cohort, @RequestParam(required = false) LocalDate weekStart) {
        adminService.clearMvp(cohort, resolveWeekStart(weekStart));
    }

    /** 컴플레인 대응 등으로 점수를 수기 보정할 때 사용 - 실제 대시보드 점수에는 아직 자동 반영되지 않는다. */
    @PostMapping("/members/{userId}/adjustments")
    public AdjustmentResponse addAdjustment(@PathVariable Long userId, @RequestBody AdjustmentRequest request) {
        return adminService.addAdjustment(userId, request.weekStart(), request.statTypeCode(), request.points(), request.reason());
    }

    @GetMapping("/members/{userId}/adjustments")
    public List<AdjustmentResponse> listAdjustments(@PathVariable Long userId) {
        return adminService.listAdjustments(userId);
    }

    /** 롤백 - 보정 내역을 삭제한다 (수정이 아니라 삭제로 처리해 감사 추적을 남긴다). */
    @DeleteMapping("/adjustments/{adjustmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAdjustment(@PathVariable Long adjustmentId) {
        adminService.deleteAdjustment(adjustmentId);
    }

    // ---- recruitment applicants (internal-only, separate from users/participants) ----

    @GetMapping("/recruitment-applicants")
    public List<RecruitmentApplicantResponse> recruitmentApplicants() {
        return adminService.listRecruitmentApplicants();
    }

    @PostMapping("/recruitment-applicants")
    public RecruitmentApplicantResponse addRecruitmentApplicant(@RequestBody RecruitmentApplicantRequest request) {
        return adminService.addRecruitmentApplicant(request);
    }

    /** 엑셀(CSV) 일괄 등록 - 프론트에서 파싱한 행들을 한 번에 받는다. */
    @PostMapping("/recruitment-applicants/bulk")
    public List<RecruitmentApplicantResponse> addRecruitmentApplicants(
            @RequestBody List<RecruitmentApplicantRequest> requests
    ) {
        return adminService.addRecruitmentApplicants(requests);
    }

    @PutMapping("/recruitment-applicants/{id}")
    public RecruitmentApplicantResponse updateRecruitmentApplicant(
            @PathVariable Long id, @RequestBody RecruitmentApplicantRequest request
    ) {
        return adminService.updateRecruitmentApplicant(id, request);
    }

    @DeleteMapping("/recruitment-applicants/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecruitmentApplicant(@PathVariable Long id) {
        adminService.deleteRecruitmentApplicant(id);
    }

    private LocalDate resolveWeekStart(LocalDate requested) {
        LocalDate date = requested != null ? requested : LocalDate.now();
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
