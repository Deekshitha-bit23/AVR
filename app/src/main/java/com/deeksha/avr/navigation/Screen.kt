package com.deeksha.avr.navigation

sealed class Screen(val route: String) {
    // Auth Screens
    object Login : Screen("login")
    object OtpVerification : Screen("otp_verification")
    object AccessRestricted : Screen("access_restricted")
    
    // Common Screens
    object ProjectSelection : Screen("project_selection")
    object NotificationList : Screen("notification_list")
    object ProjectNotifications : Screen("project_notifications/{projectId}") {
        fun createRoute(projectId: String) = "project_notifications/$projectId"
    }
    
    // User Flow Screens
    object UserDashboard : Screen("user_dashboard")
    object ExpenseList : Screen("expense_list/{projectId}") {
        fun createRoute(projectId: String) = "expense_list/$projectId"
    }
    object AddExpense : Screen("add_expense/{projectId}") {
        fun createRoute(projectId: String) = "add_expense/$projectId"
    }
    object ExpenseChat : Screen("expense_chat/{projectId}") {
        fun createRoute(projectId: String) = "expense_chat/$projectId"
    }
    object TrackSubmissions : Screen("track_submissions/{projectId}") {
        fun createRoute(projectId: String) = "track_submissions/$projectId"
    }
    object ExpenseDetail : Screen("expense_detail/{expenseId}") {
        fun createRoute(expenseId: String) = "expense_detail/$expenseId"
    }
    
    // Approver Flow
    object ApproverDashboard : Screen("approver_dashboard")
    object PendingApprovals : Screen("pending_approvals")
    object ProjectPendingApprovals : Screen("project_pending_approvals/{projectId}") {
        fun createRoute(projectId: String) = "project_pending_approvals/$projectId"
    }
    object ReviewExpense : Screen("review_expense/{expenseId}") {
        fun createRoute(expenseId: String) = "review_expense/$expenseId"
    }
    object ApproverExpenseChat : Screen("approver_expense_chat/{expenseId}") {
        fun createRoute(expenseId: String) = "approver_expense_chat/$expenseId"
    }
    
    // New Approver Project Flow
    object ApproverProjectSelection : Screen("approver_project_selection")
    object ApproverNotificationScreen : Screen("approver_notification_screen")
    object ApproverProjectDashboard : Screen("approver_project_dashboard/{projectId}") {
        fun createRoute(projectId: String) = "approver_project_dashboard/$projectId"
    }
    object ApproverReports : Screen("approver_reports/{projectId}") {
        fun createRoute(projectId: String) = "approver_reports/$projectId"
    }
    object ApproverAnalytics : Screen("approver_analytics/{projectId}") {
        fun createRoute(projectId: String) = "approver_analytics/$projectId"
    }
    object CategoryDetail : Screen("category_detail/{projectId}/{categoryName}") {
        fun createRoute(projectId: String, categoryName: String) = "category_detail/$projectId/$categoryName"
    }
    object DepartmentDetail : Screen("department_detail/{projectId}/{departmentName}") {
        fun createRoute(projectId: String, departmentName: String) = "department_detail/$projectId/$departmentName"
    }
    object OverallReports : Screen("overall_reports")
    
    // Admin Flow Screens
    object AdminDashboard : Screen("admin_dashboard")
    object ManageUsers : Screen("manage_users")
    object ManageProjects : Screen("manage_projects")
    object Reports : Screen("reports")
    
    // Production Head Flow Screens
    object ProductionHeadProjectSelection : Screen("production_head_project_selection")
    object CreateUser : Screen("create_user")
    object NewProject : Screen("new_project")
    object EditProject : Screen("edit_project/{projectId}") {
        fun createRoute(projectId: String) = "edit_project/$projectId"
    }
    
    // Production Head Approval Flow (same functionality as Approver)
    object ProductionHeadDashboard : Screen("production_head_dashboard")
    object ProductionHeadPendingApprovals : Screen("production_head_pending_approvals")
    object ProductionHeadProjectPendingApprovals : Screen("production_head_project_pending_approvals/{projectId}") {
        fun createRoute(projectId: String) = "production_head_project_pending_approvals/$projectId"
    }
    object ProductionHeadReviewExpense : Screen("production_head_review_expense/{expenseId}") {
        fun createRoute(expenseId: String) = "production_head_review_expense/$expenseId"
    }
    object ProductionHeadProjectDashboard : Screen("production_head_project_dashboard/{projectId}") {
        fun createRoute(projectId: String) = "production_head_project_dashboard/$projectId"
    }
    object ProductionHeadReports : Screen("production_head_reports/{projectId}") {
        fun createRoute(projectId: String) = "production_head_reports/$projectId"
    }
    object ProductionHeadAnalytics : Screen("production_head_analytics/{projectId}") {
        fun createRoute(projectId: String) = "production_head_analytics/$projectId"
    }
    object ProductionHeadCategoryDetail : Screen("production_head_category_detail/{projectId}/{categoryName}") {
        fun createRoute(projectId: String, categoryName: String) = "production_head_category_detail/$projectId/$categoryName"
    }
    object ProductionHeadOverallReports : Screen("production_head_overall_reports")
    object Delegation : Screen("delegation/{projectId}") {
        fun createRoute(projectId: String) = "delegation/$projectId"
    }
    
    // Chat Screens
    object ChatList : Screen("chat_list/{projectId}/{projectName}") {
        fun createRoute(projectId: String, projectName: String) = "chat_list/$projectId/$projectName"
    }
    object Chat : Screen("chat/{projectId}/{chatId}/{otherUserName}") {
        fun createRoute(projectId: String, chatId: String, otherUserName: String) = "chat/$projectId/$chatId/$otherUserName"
    }
} 