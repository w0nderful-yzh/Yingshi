import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Form, Input, message } from 'antd';
import { UserOutlined, LockOutlined, SmileOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/store/authStore';
import { BorderBeam } from '@/components/magicui/BorderBeam';

/* ------------------------------------------------------------------ */
/*  RegisterPage — 注册页 (与登录页风格100%统一)                          */
/* ------------------------------------------------------------------ */

export default function RegisterPage() {
  const navigate = useNavigate();
  const register = useAuthStore((s) => s.register);
  const [loading, setLoading] = useState(false);

  /* ---- 注册逻辑 (原有逻辑完全保留) ---- */
  const onFinish = async (values: { username: string; password: string; nickname?: string }) => {
    setLoading(true);
    try {
      await register(values.username, values.password, values.nickname);
      message.success('注册成功');
      navigate('/dashboard');
    } catch (err: any) {
      message.error(err.message || '注册失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-scene" style={{ minHeight: 'auto' }}>
      <div className="warm-glass-card" style={{ width: 400 }}>
        {/* 卡片头 */}
        <div className="warm-glass-card__header">
          <h2 className="warm-glass-card__title">注册账号</h2>
          <p className="warm-glass-card__desc">创建您的账号以开始使用平台</p>
        </div>

        {/* 卡片内容 — 注册表单 */}
        <div className="warm-glass-card__body">
          <Form name="register" onFinish={onFinish} size="large" autoComplete="off">
            <div className="warm-form-field">
              <label className="warm-form-label">用户名</label>
              <Form.Item
                name="username"
                rules={[
                  { required: true, message: '请输入用户名' },
                  { min: 3, max: 50, message: '用户名长度为3-50个字符' },
                ]}
              >
                <Input
                  prefix={<UserOutlined className="warm-input-icon" />}
                  placeholder="用户名"
                  className="warm-input"
                />
              </Form.Item>
            </div>
            <div className="warm-form-field">
              <label className="warm-form-label">密码</label>
              <Form.Item
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, max: 100, message: '密码长度为6-100个字符' },
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined className="warm-input-icon" />}
                  placeholder="密码"
                  className="warm-input"
                />
              </Form.Item>
            </div>
            <div className="warm-form-field">
              <label className="warm-form-label">确认密码</label>
              <Form.Item
                name="confirm"
                dependencies={['password']}
                rules={[
                  { required: true, message: '请确认密码' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('password') === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error('两次密码不一致'));
                    },
                  }),
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined className="warm-input-icon" />}
                  placeholder="确认密码"
                  className="warm-input"
                />
              </Form.Item>
            </div>
            <div className="warm-form-field">
              <label className="warm-form-label">昵称（可选）</label>
              <Form.Item name="nickname">
                <Input
                  prefix={<SmileOutlined className="warm-input-icon" />}
                  placeholder="昵称（可选）"
                  className="warm-input"
                />
              </Form.Item>
            </div>
            <div className="warm-glass-card__footer">
              <button
                type="submit"
                className="warm-btn warm-btn--primary"
                style={{ width: '100%' }}
                disabled={loading}
              >
                {loading ? '注册中…' : '注册'}
              </button>
            </div>
            <div className="text-center" style={{ marginTop: 16 }}>
              <Link to="/login" className="warm-link">已有账号？去登录</Link>
            </div>
          </Form>
        </div>

        {/* 边框光束 */}
        <BorderBeam duration={8} size={120} color="rgba(139, 92, 246, 0.4)" />
      </div>
    </div>
  );
}
