using SmartSolarMicrogrid.API;
using System.Text;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using SmartSolarMicrogrid.API.Middleware;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Services;
using SmartSolarMicrogrid.Infrastructure.MongoDB;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
using SmartSolarMicrogrid.Infrastructure.Security;
using MongoDB.Bson.Serialization.Conventions;

var conventionPack = new ConventionPack { new IgnoreExtraElementsConvention(true) };
ConventionRegistry.Register("IgnoreExtraElements", conventionPack, type => true);

var builder=WebApplication.CreateBuilder(args);
builder.Services.Configure<MongoDbSettings>(builder.Configuration.GetSection("MongoDbSettings"));
builder.Services.AddSingleton<MongoDbContext>();

builder.Services.AddScoped<UserRepository>();
builder.Services.AddScoped<ProsumerRepository>();
builder.Services.AddScoped<GridOperatorRepository>();
builder.Services.AddScoped<MicrogridNodeRepository>();
builder.Services.AddScoped<EnergySlotRepository>();
builder.Services.AddScoped<EnergyReservationRepository>();
builder.Services.AddScoped<QRTransactionRepository>();
builder.Services.AddScoped<SlotUnavailabilityRequestRepository>();

builder.Services.AddSingleton<PasswordHasher>();
builder.Services.AddSingleton<JwtTokenService>();

builder.Services.AddScoped<IAuthService,AuthService>();
builder.Services.AddScoped<IProsumerService,ProsumerService>();
builder.Services.AddScoped<IMicrogridNodeService,MicrogridNodeService>();
builder.Services.AddScoped<IEnergySlotService,EnergySlotService>();
builder.Services.AddScoped<IReservationService,ReservationService>();
builder.Services.AddScoped<IQRTransactionService,QRTransactionService>();
builder.Services.AddScoped<IDashboardService,DashboardService>();
builder.Services.AddScoped<IUserService,UserService>();
builder.Services.AddScoped<IOcrService,NicOcrService>();
builder.Services.AddScoped<IOperatorService,OperatorService>();

var jwtKey=builder.Configuration["Jwt:Key"]!;
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme).AddJwtBearer(o=>{
    o.TokenValidationParameters=new TokenValidationParameters{
        ValidateIssuer=true,ValidateAudience=true,ValidateLifetime=true,ValidateIssuerSigningKey=true,
        ValidIssuer=builder.Configuration["Jwt:Issuer"],ValidAudience=builder.Configuration["Jwt:Audience"],
        IssuerSigningKey=new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey))
    };
});
builder.Services.AddAuthorization();
builder.Services.AddControllers().AddJsonOptions(options => { options.JsonSerializerOptions.Converters.Add(new System.Text.Json.Serialization.JsonStringEnumConverter()); });
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c=>{
    c.SwaggerDoc("v1",new OpenApiInfo{Title="Smart Solar Microgrid API",Version="v1"});
    c.AddSecurityDefinition("Bearer",new OpenApiSecurityScheme{Type=SecuritySchemeType.Http,Scheme="bearer",BearerFormat="JWT",Name="Authorization",In=ParameterLocation.Header});
    c.AddSecurityRequirement(new OpenApiSecurityRequirement{{new OpenApiSecurityScheme{Reference=new OpenApiReference{Type=ReferenceType.SecurityScheme,Id="Bearer"}},Array.Empty<string>()}});
});
builder.Services.AddCors(o=>o.AddPolicy("Clients",p=>p.AllowAnyOrigin().AllowAnyHeader().AllowAnyMethod()));

var app=builder.Build();
app.UseCors("Clients");
app.UseMiddleware<ExceptionMiddleware>();
if(app.Environment.IsDevelopment()){app.UseSwagger();app.UseSwaggerUI();}
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();
await SeedData.RunAsync(app.Services);
app.Run();
