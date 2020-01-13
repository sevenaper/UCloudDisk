import axios from 'axios';
import * as constants from './constants';


export const changeLogin = (code) => ({
	type: constants.CHANGE_USER_LOGIN,
	code
});

export const changeLogout = (code) => ({
	type: constants.CHANGE_USER_LOGOUT,
	code
})

export const login = (username, password) => {
	return (dispatch) => {
		const formData = new FormData();
		formData.append('username', username);
		formData.append('password', password);
		axios.post('/interfaces/user/login', formData).then((res) => {
			//const status = res.status;
			const code = res.error_code;
			if (code) {
				alert("登录失败");
			} else {
				dispatch(changeLogin(code))
			}
		})
	}
};

export const logout = () => {
	return (dispatch) => {
		axios.post("/interfaces/user/logout").then((res) => {
			//const status = res.status;
			const code = res.error_code;
			if (code) {
				alert("登录失败");
			} else {
				dispatch(changeLogout())
			}
		})
	}
};