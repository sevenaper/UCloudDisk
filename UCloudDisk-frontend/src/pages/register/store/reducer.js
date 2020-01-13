import {fromJS} from 'immutable';
import * as constants from './constants';

const defaultState = fromJS({
	error_code: 0,
});


export default (state = defaultState, action) => {
	switch (action.type) {
		case constants.REPEATED_USERNAME:
			return state.set('error_code', 1002);
		case constants.ILLEGAL_USERNAME:
			return state.set('error_code', 1001);
		default:
			return state;
	}
}