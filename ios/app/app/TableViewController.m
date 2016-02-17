//
//  TableViewController.m
//  app
//
// Copyright 2016 Kii Consortium
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#import "TableViewController.h"
#import "ViewController.h"

@interface Account : NSObject
@property (nonatomic, strong) NSString* userName;
@property (nonatomic, strong) NSString* password;
+(instancetype)accountWithUserName:(NSString*)userName password:(NSString*)password;
@end

@implementation Account

+(instancetype)accountWithUserName:(NSString *)userName password:(NSString *)password{
    Account* account = [[Account alloc] init];
    
    account.userName = userName;
    account.password = password;
    
    return account;
}

@end

@interface TableViewController ()
@property (nonatomic, strong) NSArray* accounts;
@end

@implementation TableViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    _accounts = [NSArray arrayWithObjects:
                 [Account accountWithUserName:@"client001" password:@"client001"],
                 [Account accountWithUserName:@"client002" password:@"client002"],
                 [Account accountWithUserName:@"client003" password:@"client003"],
                 [Account accountWithUserName:@"client004" password:@"client004"],
                 [Account accountWithUserName:@"client005" password:@"client005"], nil];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    // Return the number of sections.
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    // Return the number of rows in the section.
    return _accounts.count;
}


- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:@"Cell" forIndexPath:indexPath];
    
    cell.textLabel.text = ((Account*)_accounts[indexPath.row]).userName;
    
    return cell;
}

-(void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath{
    [self performSegueWithIdentifier:@"Login" sender:self];
}


#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    ViewController* viewController = [segue destinationViewController];
    Account* account = [_accounts objectAtIndex:[self.tableView indexPathForSelectedRow].row];
    viewController.userName = account.userName;
    viewController.password = account.password;
}


@end
