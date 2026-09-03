export function Footer() {
  return (
    <footer className="site-footer">
      <div className="site-footer-inner container">
        <div className="site-footer-contact">
          <span className="site-footer-contact-title">문의처</span>
          <p className="site-footer-contact-lead">궁금한 점은 아래로 연락 주세요.</p>
          <p className="site-footer-contact-line">
            이메일: <a href="mailto:support.b.yourprincess@gmail.com">support.b.yourprincess@gmail.com</a>
          </p>
          <p className="site-footer-contact-line">
            인스타그램 DM: (공식 계정{" "}
            <a href="https://instagram.com/b.yourprincess" target="_blank" rel="noreferrer">
              @b.yourprincess
            </a>
            )
          </p>
          <p className="site-footer-contact-line">
            카카오톡:{" "}
            <a href="https://open.kakao.com/o/swJ7TIKi" target="_blank" rel="noreferrer">
              1:1 오픈채팅
            </a>
          </p>
        </div>
        <div className="site-footer-copyright">ⓒ 2026 PP All Rights Reserved.</div>
      </div>
    </footer>
  );
}
