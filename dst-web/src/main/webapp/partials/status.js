app.controller('StatusCtrl', function ($scope, $http, $routeParams, $timeout) {

    var updateWorkflowLoop = function() {
        $http.get('/workflow/' + $routeParams.workflowId).success(function (data) {
            $scope.workflow = data;
            promise = $timeout(updateWorkflowLoop, 5000)
        })
        .error(function (data, status) {
            console.log(data, status);
        });
    };
    var promise;
    updateWorkflowLoop();

});