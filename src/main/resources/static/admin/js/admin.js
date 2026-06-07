document.addEventListener('DOMContentLoaded', function () {
    // 삭제/상태변경 등 위험한 액션에 확인 다이얼로그
    document.querySelectorAll('[data-confirm]').forEach(function (el) {
        el.addEventListener('click', function (e) {
            if (!confirm(el.getAttribute('data-confirm'))) {
                e.preventDefault();
            }
        });
    });

    // 정지 시간 선택 토글
    var statusSelect = document.getElementById('statusSelect');
    var suspensionGroup = document.getElementById('suspensionGroup');
    if (statusSelect && suspensionGroup) {
        function toggleSuspension() {
            suspensionGroup.style.display = statusSelect.value === 'SUSPENDED' ? 'block' : 'none';
        }
        statusSelect.addEventListener('change', toggleSuspension);
        toggleSuspension();
    }
});
