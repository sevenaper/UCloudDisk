import axios from 'axios';
import * as constants from './constants';

export const errMessage = () => ({
	type: constants.ILLEGAL_USERNAME
});
export const repUsername = () => ({
	type: constants.REPEATED_USERNAME
});
export const userSignupRequest = (username, password, history) => {

		let formData = new FormData();
		formData.append('username', username);
		formData.append('password', password);
		return (dispatch) => {
			return axios.post('/interfaces/user/reg', formData).then((res) => {
					const status = res.status;
					console.log(res);
					if (status) {
						if (!res.data.status) {
							history.push('/');
						} else {
							const err_code = res.data.error_code;
							if (err_code === 1001) {
								dispatch(errMessage())
							} else if (err_code === 1002) {
								dispatch(repUsername());
							}
						}
					} else {
						alert("网络状况不太好，请稍后重试");
					}
				}
			).catch(()=>{
				alert("网络状况不太好，请稍后重试");
			});
		}
	}
;
