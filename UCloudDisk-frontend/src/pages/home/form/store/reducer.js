import {fromJS} from 'immutable';
import * as constants from './constants';

const defaultState = fromJS({
	login: false,
	error_type: 0,
});

export default (state = defaultState, action) => {
	switch (action.type) {
		case constants.CHANGE_LOGIN:
			return state.set('login', action.value);
		case constants.LOG_OUT:
			return state.set('login', action.value);
		case constants.ERROR_PASSWORD:
			return state.set('error_type', 1);
		case constants.USER_NOT_EXIST:
			return state.set('error_type', 2);
		default:
			return state;
	}
}