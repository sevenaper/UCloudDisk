import React, {
	Component
} from 'react';
import 'antd/dist/antd.css';

import {
	Provider
} from 'react-redux';
import {
	BrowserRouter,
	Route
} from 'react-router-dom';
import MyHeader from './common/header';
import Home from './pages/home';
import RegistrationForm from './pages/register';
import Detail from './pages/detail/loadable.js';
import Contact from './pages/contact/index';
import store from './store';

class App extends Component {
	render() {
		return (
			<Provider store={store}>
				<BrowserRouter>
					<div>
						<MyHeader/>
						<Route path='/' exact component={Home}/>
						<Route path='/register' exact component={RegistrationForm}/>
						<Route path='/detail' exact component={Detail}/>
						<Route path='/contact' exact component={Contact}/>
					</div>
				</BrowserRouter>
			</Provider>
		);
	}
}
export default App;