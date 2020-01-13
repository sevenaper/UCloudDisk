import {fromJS} from 'immutable';
import * as constants from './constants';

const defaultState = fromJS({
	content: '',
	files: '',
	initLoading: true,
	loading: false,
	all: '',
	treeJson: '',
	isFetching: false,
	searchFile: '',
});
const listFile = (state, action) => {
	return state.merge({
		content: action.content,
		files: action.files,
		all: action.all,
		isFetching: true,
		searchFile: action.content,
	})
};
const searchItem = (state, action) => {
	let a = [];
	//console.log(state.get("content").get(0).get("file_name"));
	let len = state.get("content").size;
	//console.log(len + "test");
	for (let i = 0; i < len; i++) {
		if (state.get("content").get(i).get("file_name").indexOf(action.value) !== -1) {
			a.push(state.get("content").get(i));
		}
	}
	return state.merge({
		searchFile: a
	});
};
const deleteItem = (state, action) => {
	let a = [];
	let b = [];
	//console.log(state.get("content").get(0).get("file_name"));
	let len1 = state.get("content").size;
	let len2 = state.get("searchFile").size;
	//console.log(len + "test");
	for (let i = 0; i < len1; i++) {
		if (state.get("content").get(i).get("file_name") !== action.filename ||
			state.get("content").get(i).get("file_path") !== action.filepath) {
			a.push(state.get("content").get(i));
		}
	}
	for (let i = 0; i < len2; i++) {
		if (state.get("searchFile").get(i).get("file_name") !== action.filename ||
			state.get("searchFile").get(i).get("file_path") !== action.filepath) {
			b.push(state.get("searchFile").get(i));
		}
	}
	return state.merge({
		searchFile: b,
		content: a,
	});
}
export default (state = defaultState, action) => {
	switch (action.type) {
		case constants.CHANGE_DETAIL:

			return state.merge({
				title: action.title,
				content: action.content
			});
		case constants.GET_FILES:
			return listFile(state, action);
		case constants.SEARCH_ITEM:
			return searchItem(state, action);
		case constants.DELETE_ITEM:
			return deleteItem(state, action);
		default:
			return state;
	}
}