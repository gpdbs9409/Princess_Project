import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getProfileStats } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";

export function ProfileHeader() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [recordCount, setRecordCount] = useState(0);
  const [totalUsers, setTotalUsers] = useState(0);

  useEffect(() => {
    if (!user) return;
    getProfileStats(user.id)
      .then((stats) => {
        setRecordCount(stats.recordCount);
        setTotalUsers(stats.totalUsers);
      })
      .catch(() => {
        // decorative stats only - a failed fetch just leaves the counts at 0
      });
  }, [user]);

  if (!user) return null;

  return (
    <div className="profile-header">
      <div className="profile-header-topbar">
        <button type="button" className="profile-header-icon-btn" onClick={() => navigate(-1)} aria-label="뒤로가기">
          ‹
        </button>
        <span className="profile-header-title">Princess Project</span>
        <span className="row" style={{ gap: 12 }}>
          <button
            type="button"
            className="profile-header-icon-btn"
            onClick={() => navigate("/my-page")}
            aria-label="마이페이지로 이동"
          >
            ⋯
          </button>
        </span>
      </div>

      <div className="profile-header-main">
        <div className="profile-header-avatar-ring">
          {user.profileImageUrl ? (
            <img src={user.profileImageUrl} alt="" className="profile-header-avatar" />
          ) : (
            <span className="profile-header-avatar profile-header-avatar-fallback">{user.nickname.slice(0, 1)}</span>
          )}
        </div>

        <div className="profile-header-stats">
          <div className="profile-header-stat">
            <strong>{recordCount}</strong>
            <span className="muted">게시물</span>
          </div>
          <div className="profile-header-stat">
            <strong>{totalUsers}</strong>
            <span className="muted">팔로워</span>
          </div>
          <div className="profile-header-stat">
            <strong>{totalUsers}</strong>
            <span className="muted">팔로잉</span>
          </div>
        </div>
      </div>

      <div className="profile-header-nickname">{user.nickname}</div>
    </div>
  );
}
