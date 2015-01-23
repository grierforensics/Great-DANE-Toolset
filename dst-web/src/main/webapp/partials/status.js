
app.controller('StatusCtrl', function ($scope) {
    $scope.email = "user@example.com";

    $scope.updates = [
        {
            'type': 'sendingAuth',
            'date':new Date()
        }
    ];

    //todo: replace simulated updates with real updates from server

    setTimeout(function(){
        $scope.updates = [
            {
                'type': 'receivedAuthResponse',
                'email': 'user@example.com',
                'approved': true,
                'date':new Date()
            }
        ];
        $scope.$apply();
    },2000);

    setTimeout(function(){
        $scope.updates.push({
                'type': 'validCert',
                'email': 'user@example.com',
                'certDetails': 'Some Cert Details',
                'date':new Date()
            });
        $scope.$apply();
    },3000);

    setTimeout(function(){
        $scope.updates.push({
                'type': 'encryptedEmailSent',
                'email': 'user@example.com',
                'date':new Date()
            });
        $scope.updates.push({
                'type': 'waiting',
                'date':new Date()
            });
        $scope.$apply();
    },3500);
});