import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/store/authStore';
import { BorderBeam } from '@/components/magicui/BorderBeam';
import { ArrowRight, Phone, PawPrint } from 'lucide-react';
import cat1 from '@/assets/cat1.jpg';
import cat2 from '@/assets/cat2.jpg';

/* ------------------------------------------------------------------ */
/*  LoginPage — 两阶段登录页 (温暖治愈风)                                 */
/*  第一阶段：展示标题动画 + "开始使用" 按钮                               */
/*  第二阶段：滑入玻璃拟态登录卡片                                        */
/* ------------------------------------------------------------------ */

export default function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [loading, setLoading] = useState(false);
  /** false = 欢迎屏, true = 登录表单 */
  const [showLogin, setShowLogin] = useState(false);

  /* ---- 登录逻辑 (原有逻辑完全保留) ---- */
  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      await login(values.username, values.password);
      message.success('登录成功');
      navigate('/dashboard');
    } catch (err: any) {
      message.error(err.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-scene">
      {/* 左上角 Logo（独立于英雄区，固定在页面左上角） */}
      <div className={`hero-logo ${showLogin ? 'hero-logo--hidden' : ''}`}>
        <PawPrint />
        <span>Pet AIoT</span>
      </div>

      {/* ====== 第一阶段：欢迎屏 (英雄区) ====== */}
      <div className={`hero-section ${showLogin ? 'hero-section--hidden' : ''}`}>
        {/* 左侧：文字内容区 */}
        <div className="hero-left">
          <h1 className="hero-title">
            全天候智能守护<br />您的专属宠物管家
          </h1>
          <p className="hero-desc">
            采用前沿 AIoT 监测技术，实时感知爱宠动态与环境状况。<br />
            无论身在何处，都能给它们最温暖、最安心的陪伴。
          </p>
          <div className="hero-actions">
            <button className="start-btn" onClick={() => setShowLogin(true)}>
              <span>开始使用</span>
              <ArrowRight className="w-5 h-5 ml-2" />
            </button>
            <button className="secondary-btn">
              <Phone className="w-5 h-5 mr-2" />
              <span>联系客服</span>
            </button>
          </div>
        </div>

        {/* 右侧：图片展示区 */}
        <div className="hero-right">
          <div className="hero-image-wrapper">
            {/* 主图片 (大胶囊) */}
            <div className="hero-image-main">
              <img src={cat1} alt="Pet monitor main" />
            </div>
            {/* 次图片 (小胶囊，右上角部分重叠) */}
            <div className="hero-image-secondary">
              <img src={cat2} alt="Pet monitor secondary" />
            </div>
          </div>
        </div>
      </div>

      {/* ====== 第二阶段：登录卡片 ====== */}
      <div className={`auth-card-wrapper ${showLogin ? 'auth-card-wrapper--visible' : ''}`}>
        <div className="warm-glass-card">
          {/* 卡片头 */}
          <div className="warm-glass-card__header">
            <h2 className="warm-glass-card__title">欢迎回来</h2>
            <p className="warm-glass-card__desc">输入账号密码以登录系统</p>
          </div>

          {/* 卡片内容 — 登录表单 */}
          <div className="warm-glass-card__body">
            <Form name="login" onFinish={onFinish} size="large" autoComplete="off">
              <div className="warm-form-field">
                <label className="warm-form-label">用户名</label>
                <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                  <Input
                    prefix={<UserOutlined className="warm-input-icon" />}
                    placeholder="请输入用户名"
                    className="warm-input"
                  />
                </Form.Item>
              </div>
              <div className="warm-form-field">
                <label className="warm-form-label">密码</label>
                <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                  <Input.Password
                    prefix={<LockOutlined className="warm-input-icon" />}
                    placeholder="请输入密码"
                    className="warm-input"
                  />
                </Form.Item>
              </div>
              <div className="warm-glass-card__footer">
                <button
                  type="button"
                  className="warm-btn warm-btn--outline"
                  onClick={() => navigate('/register')}
                >
                  注册
                </button>
                <button
                  type="submit"
                  className="warm-btn warm-btn--primary"
                  disabled={loading}
                >
                  {loading ? '登录中…' : '登录'}
                </button>
              </div>
            </Form>
          </div>

          {/* 边框光束 */}
          <BorderBeam duration={8} size={120} color="rgba(139, 92, 246, 0.4)" />
        </div>

        {/* 返回欢迎屏按钮 */}
        <button className="auth-back-btn" onClick={() => setShowLogin(false)}>
          ← 返回
        </button>
      </div>
    </div>
  );
}
