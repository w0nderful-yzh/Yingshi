export function canWriteRole(role?: string | null) {
  return role === 'ADMIN' || role === 'OPERATOR';
}

export function roleLabel(role?: string | null) {
  switch (role) {
    case 'ADMIN':
      return '管理员';
    case 'OPERATOR':
      return '运营人员';
    case 'VIEWER':
      return '只读访客';
    default:
      return role || '-';
  }
}
