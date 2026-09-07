export interface LoginFields {
  email: string;
  password: string;
}

export interface RegisterFields {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  birthday: string;
  gender: string;
}

export function validateLogin(fields: LoginFields): string {
  if (!fields.email || !fields.password) return "Vui lòng nhập đầy đủ thông tin.";
  return "";
}

export function validateRegister(fields: RegisterFields): string {
  if (Object.values(fields).some(v => !v)) return "Vui lòng điền đầy đủ thông tin.";
  return "";
}
