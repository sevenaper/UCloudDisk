import axios from 'axios';
import * as constants from './constants';

const changeLogin = () => ({
	type: constants.CHANGE_LOGIN,
	value: true
});
const loginError = () => ({
	type: constants.ERROR_PASSWORD,
});
const userNotExist = () => ({
	type: constants.USER_NOT_EXIST
})
export const logout = () => ({
	type: constants.LOG_OUT,
	value: false
});

export const login = (accout, password, history) => {
	var formData = new FormData();
	formData.append('username', accout);
	formData.append('password', password);
	return (dispatch) => {
		axios.post('/interfaces/user/login', formData).then((res) => {
			const status = res.status;
			//console.log(res);
			if (status) {
				if (!res.data.status) {
					dispatch(changeLogin());
					history.push('/detail');
					//console.log(history);
				} else if (res.data.error_code === 1003) {
					dispatch(loginError());
				} else {
					dispatch(userNotExist());
				}
			} else {
				alert("网络状况不太好，请稍后重试");
			}
		}).catch(() => {
			alert("网络状况不太好，请稍后重试");
		})
	}
};

export const userLogout = () => {
	return (dispatch) => {
		axios.post("/interfaces/user/logout").then((res) => {
			//const status = res.status;
			const error_code = res.error_code;
			if (!error_code) {
				dispatch(logout());
			}
		})
	}
};