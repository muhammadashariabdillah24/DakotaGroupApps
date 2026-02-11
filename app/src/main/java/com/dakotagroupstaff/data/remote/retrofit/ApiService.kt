package com.dakotagroupstaff.data.remote.retrofit

import com.dakotagroupstaff.data.remote.response.AgentLocationData
import com.dakotagroupstaff.data.remote.response.ApiResponse
import com.dakotagroupstaff.data.remote.response.AttendanceHistoryData
import com.dakotagroupstaff.data.remote.response.EmployeeBioData
import com.dakotagroupstaff.data.remote.response.EmployeeBioRequest
import com.dakotagroupstaff.data.remote.response.LeaveBalanceData
import com.dakotagroupstaff.data.remote.response.LeaveDetailsData
import com.dakotagroupstaff.data.remote.response.LeaveSubmissionData
import com.dakotagroupstaff.data.remote.response.LoginData
import com.dakotagroupstaff.data.remote.response.PendingApprovalData
import com.dakotagroupstaff.data.remote.response.SalarySlipData
import com.dakotagroupstaff.data.remote.response.SalarySlipsRequest
import com.dakotagroupstaff.data.remote.response.SubmitAttendanceData
import com.dakotagroupstaff.data.remote.response.SubmitAttendanceRequest
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    
    /**
     * Login endpoint
     * POST /auth/login?pt=<pt>
     */
    @POST("auth/login")
    suspend fun login(
        @Query("pt") pt: String,
        @Body loginRequest: LoginRequest
    ): ApiResponse<List<LoginData>>
    
    /**
     * Refresh Access Token
     * POST /auth/refresh-token?pt=<pt>
     */
    @POST("auth/refresh-token")
    suspend fun refreshAccessToken(
        @Query("pt") pt: String,
        @Body request: RefreshTokenRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.RefreshTokenData>
    
    /**
     * Logout and revoke refresh token
     * POST /auth/logout?pt=<pt>
     */
    @POST("auth/logout")
    suspend fun logout(
        @Query("pt") pt: String,
        @Body request: LogoutRequest
    ): ApiResponse<Any>
    
    /**
     * Get Agent Locations
     * GET /agent/locations?pt=<pt>
     */
    @GET("agent/locations")
    suspend fun getAgentLocations(
        @Query("pt") pt: String
    ): ApiResponse<List<AgentLocationData>>
    
    /**
     * Get Employee Bio
     * POST /employee/bio?pt=<pt>
     */
    @POST("employee/bio")
    suspend fun getEmployeeBio(
        @Query("pt") pt: String,
        @Body request: EmployeeBioRequest
    ): ApiResponse<List<EmployeeBioData>>

    /**
     * Get Attendance History (last 3 months)
     * POST /attendance?pt=<pt>
     */
    @POST("attendance")
    suspend fun getAttendanceHistory(
        @Query("pt") pt: String,
        @Body request: AttendanceRequest
    ): ApiResponse<List<AttendanceHistoryData>>
    
    /**
     * Submit Attendance (Check In/Out)
     * POST /attendance/submit?pt=<pt>
     * Backend returns JSON as String (text/plain) to avoid Gson deserialization issues
     * Repository will parse the String to ApiResponse<SubmitAttendanceData>
     */
    @POST("attendance/submit")
    suspend fun submitAttendance(
        @Query("pt") pt: String,
        @Body request: SubmitAttendanceRequest
    ): ApiResponse<SubmitAttendanceData>
    
    /**
     * Get Salary Slips
     * POST /salary/slips?pt=<pt>
     * Backend returns: ApiResponse<List<SalarySlipData>>
     */
    @POST("salary/slips")
    suspend fun getSalarySlips(
        @Query("pt") pt: String,
        @Body request: SalarySlipsRequest
    ): ApiResponse<List<SalarySlipData>>
    
    /**
     * Get Leave Balance
     * POST /leave/balance?pt=<pt>
     */
    @POST("leave/balance")
    suspend fun getLeaveBalance(
        @Query("pt") pt: String,
        @Body request: LeaveRequest
    ): ApiResponse<List<LeaveBalanceData>>
    
    /**
     * Get Leave Details/History
     * POST /leave/details?pt=<pt>
     */
    @POST("leave/details")
    suspend fun getLeaveDetails(
        @Query("pt") pt: String,
        @Body request: LeaveRequest
    ): ApiResponse<List<LeaveDetailsData>>
    
    /**
     * Submit Leave Request
     * POST /leave/submit?pt=<pt>
     */
    @POST("leave/submit")
    suspend fun submitLeaveRequest(
        @Query("pt") pt: String,
        @Body request: LeaveSubmissionRequest
    ): ApiResponse<LeaveSubmissionData>
    
    /**
     * Check if NIP is Supervisor
     * GET /leave/check-supervisor?pt=<pt>&nip=<nip>
     */
    @GET("leave/check-supervisor")
    suspend fun checkSupervisor(
        @Query("pt") pt: String,
        @Query("nip") nip: String
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.SupervisorCheckData>
    
    /**
     * Get Pending Approval List for Supervisor
     * GET /leave/pending-approvals?pt=<pt>&nip=<nip>
     */
    @GET("leave/pending-approvals")
    suspend fun getPendingApprovals(
        @Query("pt") pt: String,
        @Query("nip") nip: String
    ): ApiResponse<List<PendingApprovalData>>
    
    /**
     * Approve Leave Request (Same Supervisor)
     * POST /leave/approve-same-supervisor?pt=<pt>
     */
    @POST("leave/approve-same-supervisor")
    suspend fun approveLeaveSameSupervisor(
        @Query("pt") pt: String,
        @Body request: ApprovalRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.LeaveApprovalData>
    
    /**
     * Approve Leave Request (Different Supervisor)
     * POST /leave/approve-different-supervisor?pt=<pt>
     */
    @POST("leave/approve-different-supervisor")
    suspend fun approveLeaveDifferentSupervisor(
        @Query("pt") pt: String,
        @Body request: ApprovalRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.LeaveApprovalData>
    
    /**
     * Reject Leave Request
     * POST /leave/reject?pt=<pt>
     */
    @POST("leave/reject")
    suspend fun rejectLeaveRequest(
        @Query("pt") pt: String,
        @Body request: RejectionRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.LeaveRejectionData>
    
    /**
     * Get Super Atasan List
     * GET /leave/super-atasan?pt=<pt>
     */
    @GET("leave/super-atasan")
    suspend fun getSuperAtasan(
        @Query("pt") pt: String
    ): ApiResponse<List<com.dakotagroupstaff.data.remote.response.SuperAtasanData>>
    
    // ==================== LETTER OF ASSIGNMENT (SURAT TUGAS) ====================
    
    /**
     * Get Letter of Assignment Details
     * POST /delivery/letter-of-assign?pt=<pt>
     */
    @POST("delivery/letter-of-assign")
    suspend fun getLetterOfAssignment(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.LetterOfAssignRequest
    ): ApiResponse<List<com.dakotagroupstaff.data.remote.response.LetterOfAssignmentData>>
    
    /**
     * Get All Assignments List
     * POST /assignment/list?pt=<pt>
     */
    @POST("assignment/list")
    suspend fun getAssignmentList(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.LetterOfAssignRequest
    ): ApiResponse<List<com.dakotagroupstaff.data.remote.response.AssignmentListItem>>
    
    /**
     * Update GPS Location for Assignment
     * POST /assignment/update-location?pt=<pt>
     */
    @POST("assignment/update-location")
    suspend fun updateAssignmentLocation(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.UpdateLocationRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.UpdateLocationData>
    
    /**
     * Get Operational Cost Items (Master Data)
     * GET /operational-cost/items?pt=<pt>
     */
    @GET("operational-cost/items")
    suspend fun getOperationalCostItems(
        @Query("pt") pt: String
    ): ApiResponse<List<com.dakotagroupstaff.data.remote.response.OperationalCostItem>>
    
    /**
     * Get Operational Cost List by Type
     * POST /operational-cost/list?pt=<pt>
     * Types: "op" (operational), "dp" (down payment), "vcn" (voucher non-cash), "vc" (voucher)
     */
    @POST("operational-cost/list")
    suspend fun getOperationalCostList(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.OperationalCostListRequest
    ): ApiResponse<List<List<String>>>  // Returns array of arrays wrapped in ApiResponse
    
    /**
     * Save Operational Cost
     * POST /operational-cost/save?pt=<pt>
     */
    @POST("operational-cost/save")
    suspend fun saveOperationalCost(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.SaveOperationalCostRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.SaveOperationalCostData>
    
    /**
     * Check Approval Status for Operational Cost
     * POST /operational-cost/check-approval?pt=<pt>
     */
    @POST("operational-cost/check-approval")
    suspend fun checkOperationalCostApproval(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.CheckApprovalRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.ApprovalStatusData>
    
    // ==================== DELIVERY/LOPER ====================
    
    /**
     * Get Delivery List (BTT Loper)
     * GET /delivery/list?pt=<pt>
     */
    @POST("delivery/list")
    suspend fun getDeliveryList(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.DeliveryListRequest
    ): com.dakotagroupstaff.data.remote.response.DeliveryListResponse
    
    /**
     * Check Delivery Status
     * POST /delivery/check-status?pt=<pt>
     */
    @POST("delivery/check-status")
    suspend fun checkDeliveryStatus(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.CheckDeliveryStatusRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.CheckDeliveryStatusData>
    
    /**
     * Upload Delivery Photo (ePOD)
     * Returns image code for later use
     * POST /delivery/upload-photo?pt=<pt>
     */
    @POST("delivery/upload-photo")
    suspend fun uploadDeliveryPhoto(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.UploadPhotoRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.UploadPhotoData>
    
    /**
     * Update Delivery Photo Reference
     * POST /delivery/update-photo?pt=<pt>
     */
    @POST("delivery/update-photo")
    suspend fun updateDeliveryPhoto(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.UpdateDeliveryPhotoRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.UpdateDeliveryPhotoData>
    
    /**
     * Update Delivery Signature Reference
     * POST /delivery/update-signature?pt=<pt>
     */
    @POST("delivery/update-signature")
    suspend fun updateDeliverySignature(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.UpdateDeliverySignatureRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.UpdateDeliverySignatureData>
    
    /**
     * Submit Delivery Data (Photo & Signature)
     * POST /delivery/submit?pt=<pt>
     */
    @POST("delivery/submit")
    suspend fun submitDeliveryData(
        @Query("pt") pt: String,
        @Body request: com.dakotagroupstaff.data.remote.response.SubmitDeliveryRequest
    ): ApiResponse<com.dakotagroupstaff.data.remote.response.SubmitDeliveryData>
}

data class LoginRequest(
    @SerializedName("nip")
    val nip: String,
    
    @SerializedName("deviceId")
    val deviceId: String,
    
    @SerializedName("serialNumber")
    val serialNumber: String,
    
    @SerializedName("email")
    val email: String
)

data class AttendanceRequest(
    @SerializedName("nip")
    val nip: String
)

data class LeaveRequest(
    @SerializedName("nip")
    val nip: String,
    @SerializedName("tahun")
    val tahun: String
)

data class LeaveSubmissionRequest(
    @SerializedName("nip")
    val nip: String,
    @SerializedName("tgla")
    val tgla: String,
    @SerializedName("tgle")
    val tgle: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("keterangan")
    val keterangan: String,
    @SerializedName("atasan1")
    val atasan1: String,
    @SerializedName("atasan2")
    val atasan2: String,
    @SerializedName("pgaji")
    val pgaji: String = "N",
    @SerializedName("pcuti")
    val pcuti: String = "N",
    @SerializedName("pdispen")
    val pdispen: String = "N",
    @SerializedName("form")
    val form: String = "N",
    @SerializedName("obat")
    val obat: String = "0",
    @SerializedName("surat")
    val surat: String = "N"
)

data class ApprovalRequest(
    @SerializedName("nipAtasan")
    val nipAtasan: String,
    @SerializedName("leaveId")
    val leaveId: String,
    @SerializedName("approvalValue")
    val approvalValue: String,  // "Y" or "N"
    @SerializedName("potongGaji")
    val potongGaji: String,      // "Y" or "N"
    @SerializedName("potongCuti")
    val potongCuti: String,      // "Y" or "N"
    @SerializedName("dispensasi")
    val dispensasi: String       // "Y" or "N"
)

data class RejectionRequest(
    @SerializedName("nipAtasan")
    val nipAtasan: String,
    @SerializedName("leaveId")
    val leaveId: String,
    @SerializedName("activeStatus")
    val activeStatus: String  // "Y" or "N"
)
