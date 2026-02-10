package com.gotcha.domain.report.exception;

import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.exception.ErrorCode;

public class ReportException extends BusinessException {

    private ReportException(ErrorCode errorCode) {
        super(errorCode);
    }

    private ReportException(ErrorCode errorCode, String additionalInfo) {
        super(errorCode, additionalInfo);
    }

    public static ReportException notFound() {
        return new ReportException(ReportErrorCode.REPORT_NOT_FOUND);
    }

    public static ReportException notFound(Long reportId) {
        return new ReportException(ReportErrorCode.REPORT_NOT_FOUND, "reportId: " + reportId);
    }

    public static ReportException alreadyReported() {
        return new ReportException(ReportErrorCode.ALREADY_REPORTED);
    }

    public static ReportException targetNotFound() {
        return new ReportException(ReportErrorCode.TARGET_NOT_FOUND);
    }

    public static ReportException targetNotFound(String targetType, Long targetId) {
        return new ReportException(ReportErrorCode.TARGET_NOT_FOUND, targetType + " id: " + targetId);
    }

    public static ReportException cannotReportSelf() {
        return new ReportException(ReportErrorCode.CANNOT_REPORT_SELF);
    }

    public static ReportException detailRequiredForOther() {
        return new ReportException(ReportErrorCode.DETAIL_REQUIRED_FOR_OTHER);
    }

    public static ReportException unauthorizedCancel() {
        return new ReportException(ReportErrorCode.UNAUTHORIZED_CANCEL);
    }

    public static ReportException alreadyProcessed() {
        return new ReportException(ReportErrorCode.ALREADY_PROCESSED);
    }

    public static ReportException adminOnly() {
        return new ReportException(ReportErrorCode.ADMIN_ONLY);
    }

    public static ReportException invalidReasonForTarget() {
        return new ReportException(ReportErrorCode.INVALID_REASON_FOR_TARGET);
    }
}
