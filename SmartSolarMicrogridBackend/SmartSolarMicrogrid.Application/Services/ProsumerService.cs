using SmartSolarMicrogrid.Application.DTOs.Prosumer;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Validators;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
using SmartSolarMicrogrid.Infrastructure.Security;
namespace SmartSolarMicrogrid.Application.Services;
public class ProsumerService : IProsumerService {
    private readonly ProsumerRepository _pros; private readonly UserRepository _users; private readonly PasswordHasher _hasher; private readonly IOcrService _ocr;
    public ProsumerService(ProsumerRepository pros,UserRepository users,PasswordHasher hasher, IOcrService ocr){_pros=pros;_users=users;_hasher=hasher;_ocr=ocr;}
    public async Task<Prosumer> RegisterAsync(CreateProsumerDto dto){
        Validation.Required(dto.NIC,"NIC"); Validation.NIC(dto.NIC); Validation.Required(dto.Username,"Username"); Validation.Required(dto.Password,"Password");
        if((await _pros.GetAllAsync()).Any(x=>x.NIC.Equals(dto.NIC,StringComparison.OrdinalIgnoreCase))) throw new InvalidOperationException("NIC already exists.");
        if((await _users.GetAllAsync()).Any(x=>x.Username.Equals(dto.Username,StringComparison.OrdinalIgnoreCase))) throw new InvalidOperationException("Username already exists.");
        
        string? extractedNic = await _ocr.ExtractNicFromImageAsync(dto.NicFrontImageBase64, dto.NIC);

        var user=new User{Id=Guid.NewGuid().ToString(),Username=dto.Username,PasswordHash=_hasher.Hash(dto.Password),Role=UserRole.Prosumer,Status=UserStatus.Pending};
        await _users.InsertAsync(user);
        var p=new Prosumer{Id=Guid.NewGuid().ToString(),NIC=dto.NIC,FullName=dto.FullName,Email=dto.Email,Phone=dto.Phone,Address=dto.Address,UserId=user.Id,AccountStatus=UserStatus.Pending,IsDrpVerified=false,DrpVerificationRef="",NicFrontImageBase64=dto.NicFrontImageBase64,NicBackImageBase64=dto.NicBackImageBase64,ExtractedNicNumber=extractedNic};
        await _pros.InsertAsync(p); return p;
    }
    public async Task<Prosumer?> GetByNicAsync(string nic)=>(await _pros.GetAllAsync()).FirstOrDefault(x=>x.NIC.Equals(nic,StringComparison.OrdinalIgnoreCase));
    public async Task<Prosumer> UpdateAsync(string nic,UpdateProsumerDto dto){
        var p=await GetByNicAsync(nic) ?? throw new KeyNotFoundException("Prosumer not found.");
        p.FullName=dto.FullName;p.Email=dto.Email;p.Phone=dto.Phone;p.Address=dto.Address;p.UpdatedAt=DateTime.UtcNow;
        await _pros.ReplaceAsync(p.Id,p);return p;
    }
    public async Task<Prosumer> RequestDeactivationAsync(string nic)=>await SetStatusAsync(nic,"deactivate-request");
    public async Task<IReadOnlyList<Prosumer>> GetAllAsync() => (await _pros.GetAllAsync()).OrderByDescending(p=>p.CreatedAt).ToList(); public async Task<IReadOnlyList<Prosumer>> GetPendingAsync()=>(await _pros.GetAllAsync()).Where(x=>x.AccountStatus==UserStatus.Pending).ToList();
    public async Task<Prosumer> SetStatusAsync(string nic,string action){
        var p=await GetByNicAsync(nic) ?? throw new KeyNotFoundException("Prosumer not found.");
        var user=await _users.GetByIdAsync(p.UserId) ?? throw new KeyNotFoundException("User not found.");
        if(action=="activate"){p.AccountStatus=UserStatus.Active;user.Status=UserStatus.Active;}
        else if(action=="deactivate" || action=="deactivate-request"){p.AccountStatus=UserStatus.Deactivated;user.Status=UserStatus.Deactivated;}
        else if(action=="reactivate"){p.AccountStatus=UserStatus.Active;user.Status=UserStatus.Active;}
        else if(action=="reject"){p.AccountStatus=UserStatus.Rejected;user.Status=UserStatus.Rejected;}
        else if(action=="verify-nic"){p.IsDrpVerified=true;}
        else throw new ArgumentException("Invalid status action.");
        p.UpdatedAt=user.UpdatedAt=DateTime.UtcNow;await _pros.ReplaceAsync(p.Id,p);await _users.ReplaceAsync(user.Id,user);return p;
    }
}
