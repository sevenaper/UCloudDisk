import {fromJS} from "immutable";
import * as constants from "./constants";

const defaultState = fromJS({
	loginJudge: false,

});

export default (state = defaultState, action) => {
	switch (action.type) {
		case constants.CHANGE_USER_LOGIN:
			return state.set("loginJudge", true);
		case constants.CHANGE_USER_LOGOUT:
			return state.set("loginJudge", false);
		default:
			return state;
	}
};
